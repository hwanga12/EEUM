import subprocess
import re
from .nmcli import sh

def ap_up(profile: str, iface: str):
    sh(["sudo", "nmcli", "connection", "modify", profile, "connection.interface-name", iface])
    sh(["sudo", "nmcli", "connection", "up", profile])

def ap_down(profile: str):
    sh(["sudo", "nmcli", "connection", "down", profile])

def get_ipv4_addr(iface: str) -> str:
    r = sh(["ip", "-4", "addr", "show", iface], check=False)
    m = re.search(r"\binet\s+(\d+\.\d+\.\d+\.\d+)/\d+", r.stdout)
    if not m:
        return None
    return m.group(1)
