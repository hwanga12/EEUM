from fastapi import FastAPI, Query, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from .config import AP_IFACE, STA_IFACE
from .ap_manager import get_ipv4_addr
from .state import MonitorState, Event
from typing import Any, Dict
from .wifi_manager import (
        scan_wifi_wlan0,
        get_active_on_wlan0,
        list_wifi_profiles_wlan0,
        provision_connect_wlan0,
        delete_profile,
        )

class EventIn(BaseModel):
    kind: str
    device: str
    data: Dict[str, Any]

class WifiConnectIn(BaseModel):
    ssid: str
    password: str

class WifiDeleteProfileIn(BaseModel):
    name: str

def create_app(state: MonitorState) -> FastAPI:
    app = FastAPI()

    @app.get("/", response_class=HTMLResponse)
    def home():
      return """
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>WiFi Setup</title>
  <style>
    body{font-family:system-ui; margin:20px;}
    .card{border:1px solid #ddd; border-radius:10px; padding:14px; margin-bottom:12px;}
    button{padding:8px 12px; border-radius:8px; border:1px solid #ccc; background:#f7f7f7; cursor:pointer;}
    input{padding:8px; width:100%; margin-top:6px; border-radius:8px; border:1px solid #ccc;}
    select{padding:8px; width:100%; margin-top:6px; border-radius:8px; border:1px solid #ccc;}
    .row{display:flex; gap:8px;}
    .row > *{flex:1;}
    .muted{color:#666; font-size:14px;}
    pre{background:#f7f7f7; padding:10px; border-radius:8px; overflow:auto;}
  </style>
</head>
<body>
  <h2>WiFi Setup</h2>

  <div class="card">
    <div><b>Active</b>: <span id="active">(loading)</span></div>
    <div class="muted" id="scanInfo"></div>
    <div style="margin-top:10px" class="row">
      <button onclick="scan(true)">Rescan</button>
      <button onclick="loadProfiles()">Load Profiles</button>
    </div>
  </div>

  <div class="card">
    <b>Connect</b>
    <label>SSID (scan list)</label>
    <select id="ssidSelect" onchange="onSelectSSID()"></select>

    <label style="margin-top:10px; display:block;">SSID (manual)</label>
    <input id="ssidInput" placeholder="SSID"/>

    <label style="margin-top:10px; display:block;">Password</label>
    <input id="pwInput" type="password" placeholder="Password"/>

    <button style="margin-top:10px" onclick="connect()">Connect</button>
    <div class="muted" id="connectMsg"></div>
  </div>

  <div class="card">
    <b>Profiles</b>
    <pre id="profiles">(not loaded)</pre>
  </div>

<script>
async function scan(rescan=false){
  document.getElementById("scanInfo").innerText = "scanning...";
  const res = await fetch(`/wifi/scan?rescan=${rescan ? "true" : "false"}`);
  const data = await res.json();

  document.getElementById("active").innerText = data.active_ssid ?? "(none)";
  document.getElementById("scanInfo").innerText = `found ${data.aps.length} APs`;

  const sel = document.getElementById("ssidSelect");
  sel.innerHTML = "";
  for (const ap of data.aps){
    const opt = document.createElement("option");
    opt.value = ap.ssid;
    opt.textContent = `${ap.in_use ? "* " : ""}${ap.ssid} (${ap.signal}) ${ap.security}`;
    sel.appendChild(opt);
  }

  if (data.aps.length > 0){
    document.getElementById("ssidInput").value = data.aps[0].ssid;
  }
}

function onSelectSSID(){
  const sel = document.getElementById("ssidSelect");
  document.getElementById("ssidInput").value = sel.value;
}

async function connect(){
  const ssid = document.getElementById("ssidInput").value.trim();
  const pw = document.getElementById("pwInput").value;
  if(!ssid){
    alert("SSID required");
    return;
  }

  document.getElementById("connectMsg").innerText = "connecting...";
  const res = await fetch("/wifi/connect", {
    method:"POST",
    headers: {"Content-Type":"application/json"},
    body: JSON.stringify({ssid:ssid, password:pw})
  });

  const txt = await res.text();
  if(res.ok){
    document.getElementById("connectMsg").innerText = "* connected";
  }else{
    document.getElementById("connectMsg").innerText = "X failed: " + txt;
  }

  await scan();
}

async function loadProfiles(){
  const res = await fetch("/wifi/profiles");
  const data = await res.json();
  document.getElementById("profiles").innerText = JSON.stringify(data, null, 2);
}

scan();
</script>
</body>
</html>
"""

    @app.get("/ping")
    def ping():
        return {"ok": True}
    
    @app.get("/ap/ip")
    def ap_ip():
        return {"iface": AP_IFACE, "ip": get_ipv4_addr(AP_IFACE)}

    @app.get("/status")
    def status():
        return {
                "alert": state.alert,
                "last_pir_ts": state.last_pir_ts,
                "timer_running": state._timer_task is not None
        }

    @app.post("/api/event")
    async def event(data: EventIn):
        await state.queue.put(Event(**data.model_dump()))
        return {"ok": True}
    
    @app.get("/wifi/scan")
    def wifi_scan(rescan: bool = Query(False)):
        aps = scan_wifi_wlan0(rescan=rescan)
        return {
            "iface": STA_IFACE,
            "active_ssid": get_active_on_wlan0(),
            "aps": aps,
        }

    @app.get("/wifi/active")
    def wifi_active():
        return {"iface": STA_IFACE, "ssid": get_active_on_wlan0()}
    
    @app.get("/wifi/profiles")
    def wifi_profiles():
        profiles = list_wifi_profiles_wlan0()
        return {
            "iface": STA_IFACE,
            "active_ssid": get_active_on_wlan0(),
            "profiles": [p.__dict__ for p in profiles],
        }

    @app.post("/wifi/connect")
    def wifi_connect(body: WifiConnectIn):
        ssid = body.ssid.strip()
        if not ssid:
            raise HTTPException(status_code=400, detail="ssid is required")

        res = provision_connect_wlan0(ssid, body.password)
        if not res.ok:
            raise HTTPException(status_code=400, detail={
                "message": res.message,
                "new_profile": res.new_profile,
            })

        return {
            "ok": True,
            "iface": STA_IFACE,
            "ssid": ssid,
            "message": res.message,
        }

    @app.post("/wifi/profile/delete")
    def wifi_profile_delete(body: WifiDeleteProfileIn):
        name = body.name.strip()
        if not name:
            raise HTTPException(status_code=400, detail="name is required")

        try:
            delete_profile(name)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"delete failed: {e}")

        return {"ok": True, "deleted": name}

    return app
