import hashlib
import os
from urllib.parse import urlparse
import aiohttp
from app.config import PROFILE_PATH

def _guess_ext(url: str) -> str:
    try:
        path = urlparse(url).path or ""
        base = os.path.basename(path)
        _, ext = os.path.splitext(base)
        ext = (ext or "").lower()
        if ext in (".jpg", ".jpeg", ".png", ".webp", ".gif"):
            return ext
    except Exception:
        pass
    return ".img"

def profile_cache_filename(url: str) -> str:
    u = (url or "").strip()
    h = hashlib.sha1(u.encode("utf-8")).hexdigest()
    return f"{h}{_guess_ext(u)}"

def profile_cache_local_path(url: str) -> str:
    fn = profile_cache_filename(url)
    base = PROFILE_PATH or "./profile"
    return os.path.join(base, fn)

def profile_cache_public_url(url: str) -> str:
    fn = profile_cache_filename(url)
    return f"/profile/{fn}"

def _safe_write_file(path: str, data: bytes) -> bool:
    try:
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        tmp = path + ".tmp"
        with open(tmp, "wb") as f:
            f.write(data)
        os.replace(tmp, path)
        return True
    except Exception:
        try:
            if os.path.exists(path + ".tmp"):
                os.remove(path + ".tmp")
        except Exception:
            pass
        return False


def _safe_remove(path: str) -> None:
    try:
        if path and os.path.exists(path):
            os.remove(path)
    except Exception:
        pass

def remove_profile_cached(remote_url: str) -> bool:
    """
    remote_url에 대응하는 캐시 파일을 제거합니다. (best-effort)

    :param remote_url: 원본 프로필 이미지 URL
    :return: 제거 시도 결과(True=파일이 있었고 제거됨 / False=없거나 실패)
    """
    u = (remote_url or "").strip()
    if not u:
        return False

    dst = profile_cache_local_path(u)
    try:
        if os.path.exists(dst):
            _safe_remove(dst)
            _safe_remove(dst + ".tmp")
            return True
    except Exception:
        pass
    return False

def remove_profile_cached_by_public_url(public_url: str) -> bool:
    """
    /profile/<fn> 형태의 public url에 대응하는 캐시 파일을 제거합니다. (best-effort)

    :param public_url: "/profile/<filename>" 형태
    :return: 제거 시도 결과
    """
    p = (public_url or "").strip()
    if not p.startswith("/profile/"):
        return False

    fn = p.split("/profile/", 1)[-1].strip()
    if not fn:
        return False

    base = PROFILE_PATH or "./profile"
    dst = os.path.join(base, fn)

    try:
        if os.path.exists(dst):
            _safe_remove(dst)
            _safe_remove(dst + ".tmp")
            return True
    except Exception:
        pass
    return False

async def ensure_profile_cached(
    session: aiohttp.ClientSession,
    remote_url: str,
    timeout_sec: float = 10.0,
) -> str:
    """
    remote_url을 PROFILE_PATH에 캐싱합니다.
    성공/이미존재: /profile/<fn>
    실패: 원본 remote_url 반환

    :param session: aiohttp 세션
    :param remote_url: 원격 이미지 URL
    :param timeout_sec: 타임아웃(초)
    :return: 캐시된 public url 또는 원본 url
    """
    u = (remote_url or "").strip()
    if not u:
        return remote_url

    base_dir = PROFILE_PATH or "./profile"
    os.makedirs(base_dir, exist_ok=True)

    dst = profile_cache_local_path(u)
    pub = profile_cache_public_url(u)

    try:
        if os.path.exists(dst) and os.path.getsize(dst) > 0:
            return pub
    except Exception:
        pass

    if session is None or session.closed:
        return remote_url

    try:
        async with session.get(u, timeout=aiohttp.ClientTimeout(total=float(timeout_sec))) as r:
            if r.status < 200 or r.status >= 300:
                return remote_url
            data = await r.read()
            if not data:
                return remote_url

        ok = _safe_write_file(dst, data)
        return pub if ok else remote_url

    except Exception:
        return remote_url