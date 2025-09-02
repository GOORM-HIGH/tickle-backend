import sse from "k6/x/sse";
import { Counter, Rate, Trend } from "k6/metrics";
import { sleep } from "k6";

/* ==== 환경변수 ==== */
const BASE = __ENV.BASE_URL || "http://127.0.0.1:8081";
const URL = BASE + "/api/v1/notifications/connect";
const TOKEN = __ENV.TOKEN || "";
const LAST = __ENV.LAST_EVENT_ID || "";
const SESSION_SEC = Number(__ENV.SESSION_SEC || 10);

/* ==== 메트릭 ==== */
const openOk = new Rate("sse_open_ok"); // 연결 열림 여부
const stayedFull = new Rate("sse_stayed_full"); // 지정 시간 유지 여부
const connAlive = new Trend("sse_conn_alive_ms"); // 실제 유지 시간(ms)
const earlyClose = new Counter("sse_early_close"); // 중간 끊김 건수

/* ==== 실행 옵션 ==== */
export const options = {
  vus: Number(__ENV.VUS || 50), // 동시 VU 수
  iterations: Number(__ENV.ITERS || 50), // VU당 반복 1회
  thresholds: {
    sse_open_ok: ["rate>0.99"], // 99% 이상 연결 성공
    sse_stayed_full: ["rate>0.99"], // 99% 이상 풀타임 유지
  },
};

/* ==== 시나리오 ==== */
export default function () {
  const headers = {
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
    Connection: "keep-alive",
    "Accept-Encoding": "identity",
  };
  if (TOKEN) headers.Authorization = "Bearer " + TOKEN;
  if (LAST) headers["Last-Event-ID"] = LAST;

  const start = Date.now();
  let opened = false;
  let errored = false;

  try {
    sse.open(URL, { headers, timeout: `${SESSION_SEC + 5}s` }, (client) => {
      opened = true;
      console.log(`[VU ${__VU}] open`);

      // SESSION_SEC 동안 유지
      sleep(SESSION_SEC);

      try {
        client.close();
      } catch {}
    });
  } catch (e) {
    errored = true;
  }

  const aliveMs = Date.now() - start;
  connAlive.add(aliveMs);

  // 결과 기록
  openOk.add(opened && !errored);
  if (opened && !errored) {
    if (aliveMs >= SESSION_SEC * 1000) {
      stayedFull.add(1);
    } else {
      stayedFull.add(0);
      earlyClose.add(1);
    }
  }

  console.log(
    `[VU ${__VU}] opened=${opened} errored=${errored} aliveMs=${aliveMs} stayed_full=${
      aliveMs >= SESSION_SEC * 1000
    }`
  );
}
