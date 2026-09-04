import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 10,
  duration: "1m",
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api";
const AUTH_TOKEN = __ENV.AUTH_TOKEN;

if (!AUTH_TOKEN) {
  throw new Error("AUTH_TOKEN environment variable is required");
}

export default function () {
  const groupId = 1;
  const today = new Date().toISOString().split("T")[0];
  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${AUTH_TOKEN}`,
    },
  };

  const res = http.post(
    `${BASE_URL}/health/analyze?groupId=${groupId}&date=${today}`,
    null,
    params,
  );

  check(res, {
    "analysis status is 200": (r) => r.status === 200,
    "has report data": (r) => {
      try {
        return r.status === 200 && r.json().data !== null;
      } catch (error) {
        return false;
      }
    },
  });

  sleep(5);
}
