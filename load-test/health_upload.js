import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 50 },
    { duration: "1m", target: 100 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api";
const AUTH_TOKEN = __ENV.AUTH_TOKEN;

if (!AUTH_TOKEN) {
  throw new Error("AUTH_TOKEN environment variable is required");
}

export default function () {
  const groupId = Math.floor(Math.random() * 100) + 1;
  const payload = JSON.stringify([
    {
      type: "HEART_RATE",
      value: Math.floor(Math.random() * 41) + 60,
      timestamp: new Date().toISOString(),
    },
    {
      type: "STEP_COUNT",
      value: Math.floor(Math.random() * 100),
      timestamp: new Date().toISOString(),
    },
  ]);

  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${AUTH_TOKEN}`,
    },
  };

  const res = http.post(
    `${BASE_URL}/health/data?groupId=${groupId}`,
    payload,
    params,
  );

  check(res, {
    "upload status is 200": (r) => r.status === 200,
  });

  sleep(Math.random() * 2 + 1);
}
