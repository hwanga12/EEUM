import http from "k6/http";
import { check, sleep } from "k6";

// 메시지 전송 부하 테스트 (Voice Styling 프로세스 트리거 전단계)
export let options = {
  stages: [
    { duration: "30s", target: 30 },
    { duration: "1m", target: 30 },
    { duration: "30s", target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || "https://i14a105.p.ssafy.io/api";
const TOKEN =
  "Bearer REDACTED_JWT";

export default function () {
  const payload = JSON.stringify({
    content: "테스트 메시지입니다. 잘 들리시나요?",
    voiceStyle: "KIND",
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: TOKEN,
    },
  };

  let res = http.post(`${BASE_URL}/groups/1/messages`, payload, params);

  check(res, {
    "send message status is 200": (r) => r.status === 200,
    "has message id": (r) => r.json().data.id !== undefined,
  });

  sleep(2);
}
