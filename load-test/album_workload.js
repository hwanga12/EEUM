import http from "k6/http";
import { check, sleep } from "k6";

// 앨범 조회 및 업로드 준비 부하 테스트
export let options = {
  vus: 1000,
  duration: "1m",
};

const BASE_URL = __ENV.BASE_URL || "https://i14a105.p.ssafy.io/api";
const TOKEN =
  "Bearer REDACTED_JWT";

export default function () {
  const params = {
    headers: { Authorization: TOKEN },
  };

  // 1. 앨범 목록 조회
  let listRes = http.get(`${BASE_URL}/families/1/album`, params);
  check(listRes, { "get album success": (r) => r.status === 200 });

  // 2. Presigned URL 요청 (업로드 시뮬레이션 전단계)
  let urlRes = http.get(
    `${BASE_URL}/album/presigned-url?fileName=test.jpg&contentType=image/jpeg`,
    params,
  );
  check(urlRes, { "get presigned success": (r) => r.status === 200 });

  sleep(3);
}
