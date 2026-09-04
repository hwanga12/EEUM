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
  headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
};

export default function () {
  const res = http.get(`${BASE_URL}/falls/families/1`, params);

  check(res, {
    "get fall history status is 200": (r) => r.status === 200,
  });

  sleep(1);
}
