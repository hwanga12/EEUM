# fall_candidate.py
# -----------------------------------------
# 목적:
#   JSONL 관측값을 입력으로 받아
#   "넘어짐 의심(fall candidate)" 구간만 검출한다.
#
# 핵심 아이디어:
#   1) 서있다가 빠르게 아래로 무너지는지 (중심점 y 급하강)
#   2) 사람 형태가 세로 → 가로로 급변하는지 (bbox aspect 변화)
#
# 주의:
#   이 단계에서는 "확정(confirmed)"을 하지 않는다.
#   오직 '의심(candidate)'만 검출한다.
# -----------------------------------------

import json
from typing import Dict, Any, Optional, List, Tuple


# -----------------------------------------
# bbox로부터 중심점(cx, cy)과
# 세로/가로 비율(aspect = h / w)을 계산
# -----------------------------------------
def bbox_center_aspect(
    bbox: Optional[List[float]]
) -> Tuple[Optional[float], Optional[float], Optional[float]]:
    """
    bbox: [x1, y1, x2, y2] (정규화 좌표, 0~1)

    returns:
        cx      : 중심 x (0~1)
        cy      : 중심 y (0~1)
        aspect  : 높이 / 너비 (h / w)
    """
    if not bbox or len(bbox) != 4:
        return None, None, None

    x1, y1, x2, y2 = bbox

    # 분모 0 방지
    w = max(1e-6, x2 - x1)
    h = max(1e-6, y2 - y1)

    cx = (x1 + x2) * 0.5
    cy = (y1 + y2) * 0.5
    aspect = h / w

    return cx, cy, aspect


# -----------------------------------------
# 넘어짐 "의심" 감지기
# -----------------------------------------
class FallCandidateDetector:
    def __init__(
        self,
        vy_th=0.8,            # 중심점 y 급하강 임계값
        aspect_low_th=0.9,    # 눕기(aspect) 임계값
        daspect_th=-1.0,      # aspect 변화율 임계값
        min_quality=0.15      # 스켈레톤 신뢰도 최소값
    ):
        """
        vy_th:
            초당 중심점 y 변화량 (정규화 기준)
            값이 클수록 '빠른 낙하'만 감지

        aspect_low_th:
            h/w 값이 이 값보다 작아지면
            '세로 → 가로'로 판단

        daspect_th:
            초당 aspect 변화율
            급격히 눕는 동작을 잡기 위함

        min_quality:
            keypoint 평균 confidence가 이 값보다 낮으면
            관측값을 신뢰하지 않음
        """
        self.vy_th = float(vy_th)
        self.aspect_low_th = float(aspect_low_th)
        self.daspect_th = float(daspect_th)
        self.min_quality = float(min_quality)

        # 이전 프레임 정보(속도/변화량 계산용)
        self.last_ts: Optional[float] = None
        self.last_cy: Optional[float] = None
        self.last_aspect: Optional[float] = None

        # 이벤트 과다 발생 방지용 쿨다운
        self.cooldown_s = 1.0
        self.last_event_ts: Optional[float] = None

    # -----------------------------------------
    # 프레임 1개(observation 1줄) 처리
    # -----------------------------------------
    def step(self, obs: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        obs:
            JSONL에서 읽은 관측값 1개

        returns:
            넘어짐 의심 이벤트(dict) 또는 None
        """
        ts = float(obs.get("ts", 0.0))
        frame_index = int(obs.get("frame_index", -1))

        # 단일 인물 가정 → tracks[0]만 사용
        tracks = obs.get("tracks") or []
        if not tracks:
            self._reset()
            return None

        t0 = tracks[0]

        # 사람이 없거나
        if not t0.get("has_person", False):
            self._reset()
            return None

        # 관측 신뢰도가 너무 낮으면 무시
        quality = float(t0.get("quality_score") or 0.0)
        if quality < self.min_quality:
            self._reset()
            return None

        # bbox 기반 피처 계산
        cx, cy, aspect = bbox_center_aspect(t0.get("bbox"))
        if cy is None or aspect is None:
            self._reset()
            return None

        # 첫 프레임은 이전 값이 없으므로 저장만
        if self.last_ts is None:
            self.last_ts = ts
            self.last_cy = cy
            self.last_aspect = aspect
            return None

        # 시간 차
        dt = max(1e-6, ts - self.last_ts)

        # 중심점 y 속도 (아래로 갈수록 +)
        vy = (cy - self.last_cy) / dt

        # aspect 변화율 (급격히 눕는지)
        daspect = (aspect - self.last_aspect) / dt

        # ---------------------------------
        # 1차 신호 판단
        # ---------------------------------
        drop_signal = vy > self.vy_th
        rotate_signal = (
            aspect < self.aspect_low_th or
            daspect < self.daspect_th
        )

        # 이벤트 연속 발생 방지
        if self.last_event_ts is not None:
            if (ts - self.last_event_ts) < self.cooldown_s:
                drop_signal = False
                rotate_signal = False

        event = None

        # 하나라도 만족하면 "넘어짐 의심"
        if drop_signal or rotate_signal:
            self.last_event_ts = ts
            event = {
                "type": "fall_candidate",
                "ts": ts,
                "frame_index": frame_index,
                "signals": {
                    "drop_signal": drop_signal,
                    "rotate_signal": rotate_signal,
                },
                "values": {
                    "cy": cy,
                    "aspect": aspect,
                    "vy": vy,
                    "daspect": daspect,
                    "dt": dt,
                    "quality": quality,
                },
            }

        # 다음 프레임을 위해 값 갱신
        self.last_ts = ts
        self.last_cy = cy
        self.last_aspect = aspect

        return event

    # -----------------------------------------
    # 상태 초기화
    # -----------------------------------------
    def _reset(self):
        self.last_ts = None
        self.last_cy = None
        self.last_aspect = None
        self.last_event_ts = None


# -----------------------------------------
# JSONL 파일 전체를 돌려서
# 넘어짐 의심 이벤트만 추출
# -----------------------------------------
def run(
    input_path: str,
    output_path: str,
    vy_th=0.8,
    aspect_low_th=0.9,
    daspect_th=-1.0
):
    det = FallCandidateDetector(
        vy_th=vy_th,
        aspect_low_th=aspect_low_th,
        daspect_th=daspect_th
    )

    with open(input_path, "r", encoding="utf-8") as fin, \
         open(output_path, "w", encoding="utf-8") as fout:

        for line in fin:
            line = line.strip()
            if not line:
                continue

            obs = json.loads(line)
            ev = det.step(obs)

            if ev:
                fout.write(json.dumps(ev, ensure_ascii=False) + "\n")


# -----------------------------------------
# CLI 실행용
# -----------------------------------------
if __name__ == "__main__":
    import sys

    inp = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "fall_candidate_events.jsonl"

    run(inp, out)

    print(f"[OK] fall candidate events saved to {out}")
