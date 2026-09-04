import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 50 },
    { duration: "1m", target: 50 },
    { duration: "30s", target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api";
const AUTH_TOKEN = __ENV.AUTH_TOKEN;

if (!AUTH_TOKEN) {
  throw new Error("AUTH_TOKEN environment variable is required");
}

const params = {
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${AUTH_TOKEN}`,
  },
};

export default function () {
  const payload = JSON.stringify({
    content: "테스트 메시지입니다. 잘 들리시나요?",
    voiceStyle: "KIND",
  });

  const res = http.post(`${BASE_URL}/groups/1/messages`, payload, params);

  check(res, {
    "send message status is 200": (r) => r.status === 200,
    "has message id": (r) => {
      try {
        return r.status === 200 && r.json().data?.id !== undefined;
      } catch (error) {
        return false;
      }
    },
  });

  sleep(2);
}
