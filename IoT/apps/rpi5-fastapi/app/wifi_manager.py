import subprocess
import time
from dataclasses import dataclass
from typing import Optional, List, Dict
from .nmcli import sh
from .config import STA_IFACE

@dataclass
class WifiProfile:
    name: str
    ssid: Optional[str]
    iface: Optional[str]
    autoconnect: Optional[bool]
    active_device: Optional[str]

# -------- 조회 유틸 --------

def _parse_bool(s: str) -> Optional[bool]:
    if not s:
        return None
    s = s.strip().lower()
    if s in ("yes", "true", "1"):
        return True
    if s in ("no", "false", "0"):
        return False
    return None
    
def get_active_on_wlan0() -> Optional[str]:
    r = sh(["nmcli", "-g", "GENERAL.CONNECTION", "device", "show", STA_IFACE], check=False)
    conn = (r.stdout or "").strip()
    return conn if conn and conn != "--" else None

def list_wifi_profiles_wlan0() -> List[WifiProfile]:
    active = get_active_on_wlan0()

    # 전체 Wi-Fi 프로필 이름 먼저
    r = sh(["nmcli", "-t", "-f", "NAME,TYPE", "connection", "show"], check=False)
    wifi_names: List[str] = []
    for line in r.stdout.splitlines():
        # NAME:TYPE
        parts = line.split(":")
        if len(parts) >= 2 and parts[1] == "wifi":
            wifi_names.append(parts[0])

    profiles: List[WifiProfile] = []
    for name in wifi_names:
        r2 = sh(
            ["nmcli", "-g",
             "802-11-wireless.ssid,connection.interface-name,connection.autoconnect",
             "connection", "show", name],
            check=False
        )
        vals = r2.stdout.splitlines()
        ssid = vals[0].strip() if len(vals) > 0 and vals[0].strip() else None
        iface_bind = vals[1].strip() if len(vals) > 1 and vals[1].strip() else None
        autoconnect = _parse_bool(vals[2]) if len(vals) > 2 else None
        
        if iface_bind and iface_bind != STA_IFACE:
            continue

        profiles.append(WifiProfile(
            name=name,
            ssid=ssid,
            iface=iface_bind,
            autoconnect=autoconnect,
            active_device=(STA_IFACE if active == name else None)
        ))

    # wlan에서 active인 것 / wlan0로 bind된 것을 앞으로
    def _score(p: WifiProfile) -> int:
        score = 0
        if p.active_device == STA_IFACE:
            score += 10
        if p.iface == STA_IFACE:
            score += 5
        return -score

    profiles.sort(key=_score)
    return profiles

def scan_wifi_wlan0(rescan: bool = True) -> List[Dict[str, object]]:
    if rescan:
        sh(["sudo", "nmcli", "dev", "wifi", "rescan", "ifname", STA_IFACE], check=False)
        time.sleep(0.8)

    r = sh([
        "sudo", "nmcli", "-t",
        "-f", "IN-USE,SSID,SIGNAL,SECURITY",
        "device", "wifi", "list",
        "ifname", STA_IFACE
    ], check=False)

    best: Dict[str, Dict[str, object]] = {}

    for line in (r.stdout or "").splitlines():
        # IN-USE:SSID:SIGNAL:SECURITY
        parts = line.split(":", 3)
        if len(parts) != 4:
            continue

        inuse, ssid, sig, sec = parts
        ssid = ssid.strip()
        if not ssid:
            continue  # 숨김 SSID 제외

        in_use = inuse.strip() == "*"
        try:
            signal = int(sig.strip())
        except ValueError:
            signal = 0

        sec = sec.strip()

        cur = best.get(ssid)
        if cur is None or signal > int(cur["signal"]):  # type: ignore
            best[ssid] = {"ssid": ssid, "signal": signal, "security": sec, "in_use": in_use}
        else:
            if in_use:
                cur["in_use"] = True

    aps = list(best.values())
    aps.sort(key=lambda x: (0 if x["in_use"] else 1, -int(x["signal"])))
    return aps


# -------- 프로필 조작 --------

def bind_profile_to_wlan0(name: str):
    sh(["sudo", "nmcli", "connection", "modify", name, "connection.interface-name", STA_IFACE])

def delete_profile(name: str):
    sh(["sudo", "nmcli", "connection", "delete", "id", name])

def _profile_exists(name: str) -> bool:
    r = sh(["nmcli", "-g", "connection.id", "connection", "show", "id", name], check=False)
    return r.returncode == 0 and (r.stdout or "").strip() == name

def ensure_profile_named_as_ssid(ssid: str):
    if _profile_exists(ssid):
        bind_profile_to_wlan0(ssid)
        return

    sh([
        "sudo", "nmcli", "connection", "add",
        "type", "wifi",
        "ifname", STA_IFACE,
        "con-name", ssid,
        "ssid", ssid
    ])
    bind_profile_to_wlan0(ssid)

def set_profile_password(name: str, password: str):
    sh(["sudo", "nmcli", "connection", "modify", name, "802-11-wireless-security.key-mgmt", "wpa-psk"])
    sh(["sudo", "nmcli", "connection", "modify", name, "802-11-wireless-security.psk", password])

def up_profile_on_wlan0(name: str):
    sh(["sudo", "nmcli", "connection", "up", "id", name, "ifname", STA_IFACE])

def down_profile(name: str):
    sh(["sudo", "nmcli", "connection", "down", "id", name], check=False)


# -------- 프로비저닝(SSID/PW 입력 → 연결 시도) --------

@dataclass
class ProvisionResult:
    ok: bool
    message: str
    new_profile: Optional[str] = None

def provision_connect_wlan0(ssid: str, password: str) -> ProvisionResult:
    ensure_profile_named_as_ssid(ssid)
    set_profile_password(ssid, password)

    try:
        up_profile_on_wlan0(ssid)
    except subprocess.CalledProcessError as e:
        down_profile(ssid)

        msg = (e.stderr or "").strip() or "connect failed"
        return ProvisionResult(ok=False, message=msg, new_profile=ssid)

    return ProvisionResult(ok=True, message="connected", new_profile=ssid)