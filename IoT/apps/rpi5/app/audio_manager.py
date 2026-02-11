import asyncio
import logging
from dataclasses import dataclass, field
from enum import IntEnum
from typing import Callable, Iterable, Optional
from app.audio_play import AudioPlayback, start_mp3_playback
from app.config import AUDIO_PREEMPT_ONLY_FALL
from app.sync_utils import now_ts

logger = logging.getLogger(__name__)

class AudioPrio(IntEnum):
    NORMAL = 10
    SCHEDULE = 20
    VOICE = 30
    MEDICATION = 40
    FALL = 50

VOLUME_BY_KIND = {
    "normal": 1.5,
    "schedule": 1.5,
    "voice": 1.5,
    "medication": 1.5,
    "fall": 1.5,
}

@dataclass
class AudioJob:
    prio: int
    kind: str
    path: str
    created_at: float = field(default_factory=lambda: now_ts())

    ttl_sec: float = 180.0
    replace_key: Optional[str] = None
    on_done: Optional[Callable[[], None]] = None
    done_event: Optional[asyncio.Event] = None

class AudioManager:
    """
    단일 오디오 재생 관리자:
    - enqueue(AudioJob)
    - 우선순위 정책에 따라 재생/프리엠션
    - block_below_prio 설정 시 그 미만 prio는 재생 제외

    동작은 기존 구현과 동일하며, 가독성/중복만 정리한 버전입니다.
    """

    def __init__(self):
        self._cv = asyncio.Condition()
        self._queue: list[AudioJob] = []
        self._current: Optional[AudioJob] = None
        self._playback: Optional[AudioPlayback] = None
        self._task: Optional[asyncio.Task] = None
        self._supervisor_task: Optional[asyncio.Task] = None
        self._stopping = False

        self.block_below_prio: Optional[int] = None
        self.is_playing: bool = False
        self.current_prio: int = 0

        self._preempt_only_fall = AUDIO_PREEMPT_ONLY_FALL

    def _dump(self, reason: str) -> None:
        if not logger.isEnabledFor(logging.DEBUG):
            return
        cur = None
        if self._current:
            cur = f"{self._current.kind}:{int(self._current.prio)}"
        q = [
            f"{j.kind}:{int(j.prio)}" + (f"[{j.replace_key}]" if j.replace_key else "")
            for j in self._queue
        ]
        logger.debug(
            "[audio_dump] reason=%s block_below=%s current=%s qlen=%d queue=%s",
            reason,
            self.block_below_prio,
            cur,
            len(self._queue),
            q,
        )

    def start(self) -> asyncio.Task:
        if self._supervisor_task and not self._supervisor_task.done():
            return self._supervisor_task
        self._supervisor_task = asyncio.create_task(self._supervise_loop())
        return self._supervisor_task

    async def _supervise_loop(self) -> None:
        logger.info("[audio] supervisor started")
        while not self._stopping:
            try:
                self._task = asyncio.create_task(self._run_loop())
                await self._task
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception("[audio] manager crashed -> restarting")
                await asyncio.sleep(0.2)
        logger.info("[audio] supervisor stopped")

    async def stop(self) -> None:
        self._stopping = True
        async with self._cv:
            self._cv.notify_all()
        await self.stop_current()

    async def stop_current(self) -> None:
        pb = self._playback
        cur = self._current

        if pb:
            try:
                await pb.stop()
            except Exception:
                pass

        if cur:
            if cur.on_done:
                try:
                    cur.on_done()
                except Exception:
                    pass
            if cur.done_event:
                try:
                    cur.done_event.set()
                except Exception:
                    pass

    def _drop_enqueue_during_fall(self, job: AudioJob) -> bool:
        try:
            if self.block_below_prio is not None and int(self.block_below_prio) >= int(AudioPrio.FALL):
                if int(job.prio) < int(AudioPrio.FALL):
                    logger.info("[audio] drop enqueue during FALL kind=%s prio=%s", job.kind, int(job.prio))
                    if job.done_event:
                        job.done_event.set()
                    return True
        except Exception:
            pass
        return False

    def _maybe_preempt(self, new_job: AudioJob) -> None:
        if not self._current:
            return

        cur_p = int(self._current.prio)
        new_p = int(new_job.prio)

        if self._preempt_only_fall:
            if new_p >= int(AudioPrio.FALL) and cur_p < int(AudioPrio.FALL):
                logger.info(
                    "[audio] preempt(FALL) cur=%s(%s) -> new=%s(%s)",
                    self._current.kind,
                    cur_p,
                    new_job.kind,
                    new_p,
                )
                asyncio.create_task(self.stop_current())
        else:
            if new_p > cur_p:
                logger.info(
                    "[audio] preempt cur=%s(%s) -> new=%s(%s)",
                    self._current.kind,
                    cur_p,
                    new_job.kind,
                    new_p,
                )
                asyncio.create_task(self.stop_current())

    async def enqueue(self, job: AudioJob) -> None:
        if job.ttl_sec > 0 and (now_ts() - job.created_at) > job.ttl_sec:
            return

        if self._drop_enqueue_during_fall(job):
            return

        async with self._cv:
            if job.replace_key:
                self._queue = [j for j in self._queue if j.replace_key != job.replace_key]

            self._queue.append(job)
            self._dump(f"enqueue kind={job.kind} prio={int(job.prio)}")

            self._maybe_preempt(job)
            self._cv.notify_all()

            try:
                t = self._task
                if t is None:
                    logger.warning("[audio] enqueue but manager task is None (not started?)")
                elif t.done():
                    logger.warning("[audio] enqueue but manager task is DONE (crashed/stopped?)")
            except Exception:
                pass

    def _pop_next(self) -> Optional[AudioJob]:
        if not self._queue:
            return None

        best_idx = None
        best_prio = None
        best_created = None

        for i, j in enumerate(self._queue):
            if self.block_below_prio is not None and int(j.prio) < int(self.block_below_prio):
                continue

            p = int(j.prio)
            c = float(j.created_at)
            if best_idx is None:
                best_idx, best_prio, best_created = i, p, c
                continue

            if p > best_prio:
                best_idx, best_prio, best_created = i, p, c
                continue

            if p == best_prio and c < best_created:
                best_idx, best_prio, best_created = i, p, c

        if best_idx is None:
            return None
        return self._queue.pop(best_idx)

    async def _run_loop(self) -> None:
        logger.info("[audio] manager started")
        try:
            while not self._stopping:
                async with self._cv:
                    while not self._stopping:
                        nxt = self._pop_next()
                        if nxt:
                            self._current = nxt
                            self._dump(f"pop_next -> {nxt.kind}:{int(nxt.prio)}")
                            break
                        self._dump("wait_no_playable_job")
                        await self._cv.wait()

                if self._stopping:
                    break

                job = self._current
                if not job:
                    continue

                if job.ttl_sec > 0 and (now_ts() - job.created_at) > job.ttl_sec:
                    logger.info("[audio] drop expired kind=%s", job.kind)
                    if job.done_event:
                        job.done_event.set()
                    self._current = None
                    continue

                try:
                    self.is_playing = True
                    self.current_prio = int(job.prio)

                    volume = float(VOLUME_BY_KIND.get(job.kind, 1.0))
                    logger.debug("[audio] start kind=%s prio=%s path=%s volume=%.2f", job.kind, int(job.prio), job.path, volume)

                    t0 = now_ts()
                    logger.info("[audio_timing] start_call kind=%s path=%s", job.kind, job.path)

                    self._playback = await start_mp3_playback(job.path, volume=volume)

                    logger.info("[audio_timing] started dt=%.3fs kind=%s", now_ts() - t0, job.kind)

                    await self._playback.wait()

                    logger.info("[audio_timing] finished total_dt=%.3fs kind=%s", now_ts() - t0, job.kind)

                except asyncio.CancelledError:
                    raise
                except Exception as e:
                    logger.warning("[audio] play failed kind=%s err=%s", job.kind, e)
                finally:
                    self._playback = None
                    self._current = None
                    self.is_playing = False
                    self.current_prio = 0

                    if job.on_done:
                        try:
                            job.on_done()
                        except Exception:
                            pass
                    if job.done_event:
                        try:
                            job.done_event.set()
                        except Exception:
                            pass

        except asyncio.CancelledError:
            raise
        except Exception:
            logger.exception("[audio] manager fatal error")
            raise
        finally:
            logger.info("[audio] manager stopped")

    async def cancel(
        self,
        *,
        kind: str | None = None,
        replace_key: str | None = None,
    ) -> dict:
        removed = 0
        stopped_current = False

        async with self._cv:
            before = len(self._queue)

            def _match(j: AudioJob) -> bool:
                if kind is not None and j.kind != kind:
                    return False
                if replace_key is not None and j.replace_key != replace_key:
                    return False
                return True

            self._queue = [j for j in self._queue if not _match(j)]
            removed = before - len(self._queue)
            self._dump(f"cancel kind={kind} rk={replace_key} removed={removed}")

            cur = self._current
            if cur and _match(cur):
                stopped_current = True
                asyncio.create_task(self.stop_current())

            self._cv.notify_all()

        return {"removed": int(removed), "stopped_current": bool(stopped_current)}

    async def cancel_many(
        self,
        *,
        kinds: Iterable[str] | None = None,
        replace_keys: Iterable[str] | None = None,
    ) -> dict:
        kinds_set = set(kinds or [])
        rks_set = set(replace_keys or [])

        removed = 0
        stopped_current = False

        async with self._cv:
            before = len(self._queue)

            def _match(j: AudioJob) -> bool:
                if kinds_set and j.kind not in kinds_set:
                    return False
                if rks_set and (j.replace_key not in rks_set):
                    return False
                return True

            self._queue = [j for j in self._queue if not _match(j)]
            removed = before - len(self._queue)
            self._dump(f"cancel_many kinds={list(kinds_set)} rks={list(rks_set)} removed={removed}")

            cur = self._current
            if cur and _match(cur):
                stopped_current = True
                asyncio.create_task(self.stop_current())

            self._cv.notify_all()

        return {"removed": int(removed), "stopped_current": bool(stopped_current)}