import http from "k6/http";
import { check, sleep } from "k6";

// 낙상 이력 조회 (응급 상황 데이터 조회 부하 테스트)
export let options = {
  stages: [
    { duration: "30s", target: 50 },
    { duration: "1m", target: 50 },
    { duration: "30s", target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || "https://i14a105.p.ssafy.io/api";
const TOKEN =
  "Bearer REDACTED_JWT";

export default function () {
  const params = {
    headers: { Authorization: TOKEN },
  };

  let res = http.get(`${BASE_URL}/falls/families/1`, params);

  check(res, {
    "get fall history status is 200": (r) => r.status === 200,
  });

  sleep(1);
}
