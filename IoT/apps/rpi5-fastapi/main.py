import time
import asyncio
import uvicorn
from app.config import AP_PROFILE, AP_IFACE, HOST, PORT
from app.ap_manager import ap_up, get_ipv4_addr
from app.state import MonitorState
from app.api import create_app
from app.consumer import consume_events

def main():
    ap_up(AP_PROFILE, AP_IFACE)
    time.sleep(1)
    ap_ip = get_ipv4_addr(AP_IFACE)
    print(f"[AP] iface={AP_IFACE}, ip={ap_ip}")
    
    state = MonitorState()
    app = create_app(state)
    config = uvicorn.Config(app, host=HOST, port=PORT)
    server = uvicorn.Server(config)

    async def runner():
        consumer_task = asyncio.create_task(consume_events(state))
        try:
            await server.serve()
        finally:
            consumer_task.cancel()

    asyncio.run(runner())

if __name__ == "__main__":
    main()
