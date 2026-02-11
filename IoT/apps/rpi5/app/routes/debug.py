from typing import Literal
from fastapi import FastAPI
from pydantic import BaseModel
from app.api_common import queue_put_drop_oldest
from app.state import Command, Event, MonitorState
from app.sync_utils import now_ts

class DebugFallReq(BaseModel):
    level: int = 1
    device_id: str | None = None
    location_id: str | None = None

class DebugAlarmReq(BaseModel):
    kind: Literal["medication", "schedule"] = "schedule"
    content: str = "Test alarm"
    sent_at: float | None = None
    msg_id: str | None = None

def register(app: FastAPI, state: MonitorState, *, enabled: bool) -> None:
    """
    디버그 전용 라우트를 등록합니다(옵션).
    :param app: FastAPI
    :param state: MonitorState
    :param enabled: 활성화 여부
    :return: None
    """
    if not enabled:
        return

    @app.post("/debug/fall/trigger")
    async def debug_fall_trigger(body: DebugFallReq):
        device_id = (body.device_id or state.device_id or "EEUM-DEBUG").strip()
        now = now_ts()

        event = Event(
            kind="fall",
            device_id=device_id,
            data={"event": "fall_detected", "level": int(body.level or 1), "location_id": body.location_id},
            detected_at=now,
        )
        queue_put_drop_oldest(state.queue, event)
        return {"ok": True, "queued": True, "device_id": device_id, "ts": now}

    @app.post("/debug/alarm/trigger")
    async def debug_alarm_trigger(body: DebugAlarmReq):
        device_id = (state.device_id or "EEUM-DEBUG").strip()
        now = now_ts()

        payload = {
            "kind": body.kind,
            "content": body.content,
            "sent_at": float(body.sent_at) if body.sent_at is not None else float(now),
        }
        if body.msg_id:
            payload["msg_id"] = body.msg_id

        topic = f"eeum/device/{device_id}/alarm"
        cmd = Command(topic=topic, payload=payload)
        queue_put_drop_oldest(state.cmd_queue, cmd)
        return {"ok": True, "queued": True, "topic": topic, "payload": payload}
