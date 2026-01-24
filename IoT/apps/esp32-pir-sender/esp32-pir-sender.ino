#include <WiFi.h>
#include <HTTPClient.h>

// ====== AP 정보 ======
static const char* AP_SSID = "A105-RPI-PROV";
static const char* AP_PASS = "A1051234";

// ====== 서버 ======
static const char* SERVER_HOST = "192.168.4.1";
static const uint16_t SERVER_PORT = 8080;
static const char* EVENT_ENDPOINT = "/api/event";

// ====== 디바이스 ======
static const char* DEVICE_NAME = "E-00000000";

// ====== PIR 핀(GPIO27) ======
static const int PIR_PIN = 27;

// ====== 디바운스/재전송 방지 ======
static const uint32_t DEBOUNCE_MS = 200;
uint32_t lastChangeMs = 0;
int lastState = LOW;

static void connectWifi() {
  Serial.printf("[WiFi] Connecting to %s\n", AP_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);

  WiFi.disconnect(true, true);
  delay(500);
  
  WiFi.begin(AP_SSID, AP_PASS);

  uint32_t start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
    Serial.print(".");
    if (millis() - start > 20000) {
      Serial.println("\n[WiFi] Timeout. Retry...");
      WiFi.disconnect(true);
      delay(300);
      WiFi.begin(AP_SSID, AP_PASS);
      start = millis();
    }
  }
  Serial.println("\n[WiFi] Connected!");
  Serial.print("[WiFi] IP: "); Serial.println(WiFi.localIP());
  Serial.print("[WiFi] GW: "); Serial.println(WiFi.gatewayIP());
}

static bool postPirEvent(int value1) {
  if (WiFi.status() != WL_CONNECTED) connectWifi();

  String url = String("http://") + SERVER_HOST + ":" + SERVER_PORT + EVENT_ENDPOINT;

  String body = "{";
  body += "\"kind\":\"pir\",";
  body += "\"device\":\"" + String(DEVICE_NAME) + "\",";
  body += "\"data\":{";
  body += "\"event\":\"motion\",";
  body += "\"value\":" + String(value1);
  body += "}}";

  HTTPClient http;
  http.setConnectTimeout(3000);
  http.setTimeout(5000);

  Serial.printf("[POST] %s\n", url.c_str());
  Serial.printf("[POST] %s\n", body.c_str());

  if (!http.begin(url)) {
    Serial.println("[POST] begin failed");
    return false;
  }

  http.addHeader("Content-Type", "application/json");
  int code = http.POST((uint8_t*)body.c_str(), body.length());
  String resp = http.getString();
  http.end();

  Serial.printf("[POST] code=%d resp=%s\n", code, resp.c_str());
  return (code > 0 && code < 400);
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(PIR_PIN, INPUT_PULLUP);

  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  delay(500);

  int n = WiFi.scanNetworks();
  Serial.printf("[WiFi] scan=%d\n", n);
  for (int i=0; i<n; i++){
    Serial.printf("%d) %s RSSI=%d CH=%d ENC=%d\n",
      i,
      WiFi.SSID(i).c_str(),
      WiFi.RSSI(i),
      WiFi.channel(i),
      WiFi.encryptionType(i));
  }
  connectWifi();

  lastState = digitalRead(PIR_PIN);
  lastChangeMs = millis();
  Serial.printf("[PIR] initial=%d\n", lastState);
}

void loop() {
  int s = digitalRead(PIR_PIN);
  uint32_t now = millis();

  // 상태 변화(엣지) + 디바운스
  if (s != lastState && (now - lastChangeMs) > DEBOUNCE_MS) {
    lastState = s;
    lastChangeMs = now;c
    Serial.printf("[PIR] changed -> %d\n", s);

    // 움직임 감지(HIGH)일 때만 1회 전송
    if (s == HIGH) {
      postPirEvent(1);
    } else{
      postPirEvent(0);
    }
  }

  delay(10);
}
