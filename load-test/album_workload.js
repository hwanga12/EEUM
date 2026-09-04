import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 1000,
  duration: "1m",
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
  const listRes = http.get(`${BASE_URL}/families/1/album`, params);
  check(listRes, { "get album success": (r) => r.status === 200 });

  const urlRes = http.get(
    `${BASE_URL}/album/presigned-url?fileName=test.jpg&contentType=image/jpeg`,
    params,
  );
  check(urlRes, { "get presigned success": (r) => r.status === 200 });

  sleep(3);
}
