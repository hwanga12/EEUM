import asyncio
import logging
import os
from typing import Any, Optional

from app.profile_cache import ensure_profile_cached
from app.sse_fanout import fanout_nowait
from app.sync_utils import now_ts

logger = logging.getLogger(__name__)


def _build_slide_sort_key(item: dict) -> tuple:
    """
    슬라이드 정렬 키를 생성합니다.
    - takenAt이 None이면 뒤로 보냅니다.
    - 그 외에는 takenAt 오름차순(문자열 기준), id 내림차순으로 보조 정렬합니다.

    :param item: album item dict
    :return: 정렬 키(tuple)
    """
    taken_at = item.get("takenAt")
    taken_at_is_none = taken_at is None
    photo_id = int(item.get("id") or 0)
    return (taken_at_is_none, str(taken_at or ""), -photo_id)


def rebuild_playlist(state) -> None:
    """
    state.album_cache를 기반으로 슬라이드 재생목록을 재구성합니다.

    정책:
    - cache가 비었으면 playlist=[]
    - 정렬 정책은 _build_slide_sort_key만 바꾸면 됩니다.
    - playlist 변경 후 slide_index가 범위를 벗어나면 0으로 보정합니다.

    :param state: MonitorState
    :return: None
    """
    items = list((getattr(state, "album_cache", None) or {}).values())
    items.sort(key=_build_slide_sort_key)

    playlist = []
    for it in items:
        if isinstance(it, dict) and "id" in it:
            try:
                playlist.append(int(it["id"]))
            except Exception:
                continue

    state.slide_playlist = playlist

    if not state.slide_playlist:
        state.slide_index = 0
        logger.warning("[slideshow] playlist empty (album_cache=%d)", len(items))
        return

    if state.slide_index < 0 or state.slide_index >= len(state.slide_playlist):
        state.slide_index = 0

    logger.info("[slideshow] playlist rebuilt count=%d", len(state.slide_playlist))


def get_current_item(state) -> Optional[dict]:
    """
    현재 slide_index가 가리키는 album item(raw)을 반환합니다.

    :param state: MonitorState
    :return: album item dict 또는 None
    """
    if not state.slide_playlist:
        return None

    try:
        photo_id = int(state.slide_playlist[state.slide_index])
    except Exception:
        return None

    return (getattr(state, "album_cache", None) or {}).get(photo_id)


def _map_local_path_to_public_url(local_path: str) -> str:
    filename = os.path.basename(str(local_path))
    return f"/album/{filename}"


def normalize_item(raw: dict | None) -> dict | None:
    """
    album raw row(dict)를 슬라이드 공통 포맷으로 정규화합니다.

    반환 포맷:
      {id, url, description, takenAt}

    :param raw: album item raw dict
    :return: 정규화된 dict 또는 None
    """
    if not raw:
        return None

    local_path = raw.get("local_path")
    if local_path:
        url = _map_local_path_to_public_url(str(local_path))
    else:
        url = raw.get("url")

    return {
        "id": raw.get("id"),
        "url": url,
        "description": raw.get("description"),
        "takenAt": raw.get("takenAt"),
    }


def _normalize_user_id(user_id: Any) -> Optional[int]:
    try:
        return int(user_id) if user_id is not None else None
    except Exception:
        return None


def _get_member_from_cache(state, user_id: int) -> Optional[dict]:
    cache = getattr(state, "member_cache", None) or {}
    try:
        member = cache.get(user_id)
        return dict(member) if isinstance(member, dict) else None
    except Exception:
        return None


def _put_member_to_cache(state, user_id: int, member: dict) -> None:
    try:
        if getattr(state, "member_cache", None) is None:
            state.member_cache = {}
        state.member_cache[user_id] = dict(member)
    except Exception:
        return


async def _ensure_profile_url_cached_if_possible(state, profile_url: str) -> str:
    session = getattr(state, "http_session", None)
    if not profile_url:
        return profile_url
    if session is None or getattr(session, "closed", True):
        return profile_url

    try:
        return await ensure_profile_cached(session, profile_url, timeout_sec=8.0)
    except Exception:
        return profile_url


async def build_sender(state, user_id: Any) -> dict:
    """
    sender 정보를 구성합니다. (cache-first)

    처리 순서:
    1) state.member_cache에서 조회
    2) 없으면 state.member_repo(DB)에서 조회 후 캐시에 저장
    3) profile_image_url은 가능하면 /profile/... 로컬 캐시 URL로 치환

    반환 규격:
      {"user_id": int|None, "name": str, "profile_image_url": str}

    :param state: MonitorState
    :param user_id: 사용자 ID(정수 변환 시도)
    :return: sender dict
    """
    uid = _normalize_user_id(user_id)
    if uid is None:
        return {"user_id": None, "name": "", "profile_image_url": ""}

    member = _get_member_from_cache(state, uid)

    if member is None and getattr(state, "member_repo", None):
        try:
            member = state.member_repo.get(uid) or None
        except Exception:
            member = None

        if isinstance(member, dict):
            _put_member_to_cache(state, uid, member)

    name = str((member or {}).get("name") or "")
    profile_url = str((member or {}).get("profile_image_url") or "")

    profile_url = await _ensure_profile_url_cached_if_possible(state, profile_url)

    return {"user_id": uid, "name": name, "profile_image_url": profile_url}


async def build_album_item(state, raw: dict | None) -> dict | None:
    """
    슬라이드/부트/상태 조회에서 공통으로 쓰는 AlbumItem을 생성합니다.

    반환 포맷:
      {id, url, description, takenAt, sender}

    :param state: MonitorState
    :param raw: album raw dict
    :return: AlbumItem dict 또는 None
    """
    item = normalize_item(raw)
    if item is None:
        return None

    uid = raw.get("user_id") if raw else None
    item["sender"] = await build_sender(state, uid)
    return item


async def _build_slide_payload_under_lock(state, reason: str) -> dict:
    """
    slide_lock을 잡고 slide payload를 생성합니다.

    :param state: MonitorState
    :param reason: 이벤트 발생 이유
    :return: payload dict
    """
    state.slide_seq += 1
    seq = state.slide_seq

    raw = get_current_item(state)
    item = await build_album_item(state, raw)

    if item is None:
        logger.debug(
            "[slideshow] emit_slide item=None reason=%s playlist_len=%d",
            reason,
            len(state.slide_playlist),
        )

    return {"ts": now_ts(), "seq": seq, "item": item, "reason": reason}


def _wake_slide_timer(state) -> None:
    try:
        state.slide_tick_event.set()
    except Exception:
        return


async def emit_slide(state, reason: str) -> None:
    """
    SSE slide 이벤트를 모든 subscriber에게 push합니다.

    :param state: MonitorState
    :param reason: 이벤트 발생 이유
    :return: None
    """
    async with state.slide_lock:
        payload = await _build_slide_payload_under_lock(state, reason)
        fanout_nowait(state.slide_subscribers, payload)

    _wake_slide_timer(state)


def _advance_index(state, delta: int) -> None:
    if not state.slide_playlist:
        state.slide_index = 0
        return

    n = len(state.slide_playlist)
    state.slide_index = (state.slide_index + int(delta)) % n


async def next_slide(state, reason: str = "next") -> None:
    """
    다음 슬라이드로 이동한 뒤 slide 이벤트를 emit 합니다.

    :param state: MonitorState
    :param reason: 이벤트 발생 이유
    :return: None
    """
    async with state.slide_lock:
        _advance_index(state, +1)
    await emit_slide(state, reason)


async def prev_slide(state, reason: str = "prev") -> None:
    """
    이전 슬라이드로 이동한 뒤 slide 이벤트를 emit 합니다.

    :param state: MonitorState
    :param reason: 이벤트 발생 이유
    :return: None
    """
    async with state.slide_lock:
        _advance_index(state, -1)
    await emit_slide(state, reason)


async def set_playing(state, playing: bool, interval_sec: Optional[float] = None) -> None:
    """
    재생/일시정지 상태 및 재생 간격을 설정합니다.

    :param state: MonitorState
    :param playing: 재생 여부
    :param interval_sec: 재생 간격(초). None이면 변경하지 않습니다.
    :return: None
    """
    async with state.slide_lock:
        state.slide_playing = bool(playing)

        if interval_sec is not None:
            try:
                v = float(interval_sec)
                if v > 0:
                    state.slide_interval_sec = v
            except Exception:
                pass

    _wake_slide_timer(state)


async def slideshow_timer_loop(state) -> None:
    """
    슬라이드 자동 전환 타이머 루프입니다.
    - play 상태면 interval마다 next로 전환
    - pause 상태면 제어 이벤트가 올 때까지 대기
    - 어떤 이유로든 slide_tick_event가 set되면 즉시 깨워 drift를 줄입니다.

    :param state: MonitorState
    :return: None
    """
    while True:
        if getattr(state, "shutting_down", False):
            return

        if not state.slide_playing:
            await state.slide_tick_event.wait()
            state.slide_tick_event.clear()
            continue

        interval = float(state.slide_interval_sec or 60.0)

        try:
            await asyncio.wait_for(state.slide_tick_event.wait(), timeout=interval)
            state.slide_tick_event.clear()
            continue
        except asyncio.TimeoutError:
            pass

        if getattr(state, "shutting_down", False):
            return
        if not state.slide_playing:
            continue

        await next_slide(state, reason="timer")
