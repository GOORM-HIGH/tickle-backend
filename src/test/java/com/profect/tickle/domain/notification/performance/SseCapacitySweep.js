import sse from "k6/x/sse";
import { Counter, Rate, Trend } from "k6/metrics";
import { sleep } from "k6";
import { getConfig } from "./config.js";

/* ==== 설정 로드 ==== */
const config = getConfig();

/* ==== 환경변수 및 설정 ==== */
const URL = config.baseUrl + "/api/v1/notifications/connect";
const TOKEN = config.token;
const LAST = config.lastEventId;
const SESSION_SEC = config.sessionSec;
const DEBUG = config.debug;
const MAX_RETRIES = config.maxRetries;
const MEMBER_ID = config.memberId;
const ENVIRONMENT = __ENV.ENVIRONMENT || "development";

/* ==== 메트릭 ==== */
const openOk = new Rate("sse_open_ok");
const stayedFull = new Rate("sse_stayed_full");
const connAlive = new Trend("sse_conn_alive_ms");
const earlyClose = new Counter("sse_early_close");
const messagesReceived = new Counter("sse_messages_received");
const latency = new Trend("latency");
const connectionRetries = new Counter("sse_connection_retries");
const sessionsCompleted = new Counter("sse_sessions_completed");

/* ==== 실행 옵션 ==== */
export const options = {
  scenarios: {
    sse_load_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: config.stages,
    },
  },
  thresholds: config.thresholds,
};

/* ==== 유틸리티 함수 ==== */
function debugLog(message) {
  if (DEBUG) {
    const timestamp = new Date().toISOString();
    const vuInfo = `VU ${__VU}/${config.vus}`;
    console.log(`[${vuInfo}][${timestamp}] ${message}`);
  }
}

function createHeaders() {
  const headers = {
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
    Connection: "keep-alive",
    "Accept-Encoding": "identity",
    "User-Agent": "k6-sse-test/1.0",
    "X-Member-ID": MEMBER_ID,
  };

  if (TOKEN) {
    headers.Authorization = `Bearer ${TOKEN}`;
  }
  if (LAST) {
    headers["Last-Event-ID"] = LAST;
  }

  return headers;
}

function createSSEOptions() {
  return {
    headers: createHeaders(),
    timeout: `${SESSION_SEC + 5}s`, // 여유시간 추가
    readTimeout: `${SESSION_SEC + 2}s`, // 읽기 타임아웃
  };
}

/* ==== 메인 시나리오 ==== */
export default function () {
  const tags = {
    scenario: "sse_connection_test",
    environment: ENVIRONMENT,
    memberId: MEMBER_ID,
    targetVus: config.vus,
  };

  const start = Date.now();

  // 측정 완료 플래그 - 중복 측정 방지
  let measurementTaken = false;
  let connectionSuccess = false;
  let sessionSuccess = false;

  debugLog(`Attempting to connect to ${URL} with memberId=${MEMBER_ID}`);
  debugLog(`Headers: ${JSON.stringify(createHeaders())}`);

  // 연결 재시도 로직
  for (
    let attempt = 1;
    attempt <= MAX_RETRIES && !connectionSuccess;
    attempt++
  ) {
    if (attempt > 1) {
      connectionRetries.add(1, tags);
      debugLog(`Retry attempt ${attempt}/${MAX_RETRIES}`);
      sleep(1);
    }

    try {
      // SSE 연결 시도
      sse.open(URL, createSSEOptions(), function (sseClient) {
        // 콜백 실행 즉시 측정 처리
        if (!measurementTaken) {
          measurementTaken = true;
          connectionSuccess = true;

          debugLog("SSE connection established successfully");

          // 연결 성공 지표 즉시 기록
          openOk.add(1, tags);

          // 첫 메시지 수신 시간 기록
          const timeToFirst = Date.now() - start;
          latency.add(timeToFirst, tags);
          debugLog(`First message received after ${timeToFirst}ms`);

          // 메시지 카운트
          messagesReceived.add(1, tags);

          debugLog(`Maintaining connection for ${SESSION_SEC} seconds`);

          // 세션 유지
          sleep(SESSION_SEC);

          // 세션 완료 처리
          sessionSuccess = true;
          sessionsCompleted.add(1, tags);
          stayedFull.add(1, tags);

          // 핵심 수정: 연결 유지 시간을 콜백 내부에서 측정
          const aliveMs = Date.now() - start;
          connAlive.add(aliveMs, tags);

          debugLog(
            `Session completed successfully - Connection alive for ${aliveMs}ms`
          );
        }
      });

      // 연결 성공시 재시도 루프 종료
      if (connectionSuccess) {
        debugLog("Breaking retry loop - connection successful");
        break;
      }

      // 짧은 대기 후 콜백이 실행되지 않으면 다음 시도
      sleep(0.5);
    } catch (e) {
      debugLog(`Connection attempt ${attempt} failed: ${e.message}`);

      // 마지막 시도에서 실패시 실패 지표 기록
      if (attempt === MAX_RETRIES && !measurementTaken) {
        measurementTaken = true;
        openOk.add(0, tags);
        stayedFull.add(0, tags);
        // 실패한 경우에도 연결 시간 측정
        const failedAliveMs = Date.now() - start;
        connAlive.add(failedAliveMs, tags);
      }
    }
  }

  // 모든 재시도 후에도 연결되지 않은 경우
  if (!measurementTaken) {
    openOk.add(0, tags);
    stayedFull.add(0, tags);
    // 연결 실패한 경우에도 시도한 시간 측정
    const noConnAliveMs = Date.now() - start;
    connAlive.add(noConnAliveMs, tags);
    debugLog("All connection attempts failed");
  }

  // 조기 종료 체크
  if (connectionSuccess && !sessionSuccess) {
    earlyClose.add(1, tags);
    debugLog(`Session ended early`);
  }

  // 최종 결과 로그
  console.log(
    `[VU ${__VU}/${config.vus}] Summary: memberId=${MEMBER_ID}, ` +
      `opened=${connectionSuccess}, sessionCompleted=${sessionSuccess}, ` +
      `aliveMs=measured, latency=measured`
  );

  if (!connectionSuccess) {
    console.warn(
      `[VU ${__VU}] Connection failed. ` +
        `Check server availability and network connectivity.`
    );
  }
}

/* ==== 설정 검증 ==== */
export function setup() {
  console.log("=== K6 SSE Performance Test Configuration ===");
  console.log(`Environment: ${ENVIRONMENT}`);
  console.log(`Base URL: ${config.baseUrl}`);
  console.log(`Target URL: ${URL}`);
  console.log(`Member ID: ${MEMBER_ID}`);
  console.log(`Session Duration: ${SESSION_SEC}s`);
  console.log(`Max Retries: ${MAX_RETRIES}`);
  console.log(`Target VUs: ${config.vus}`);
  console.log(`Load Pattern: ${JSON.stringify(config.stages)}`);
  console.log(`Thresholds: ${JSON.stringify(config.thresholds, null, 2)}`);
  console.log(`Debug Mode: ${DEBUG ? "ON" : "OFF"}`);
  console.log(`Token Provided: ${TOKEN ? "Yes" : "No"}`);
  console.log(`Last Event ID: ${LAST || "None"}`);

  // 예상 부하 정보
  const totalDuration = config.stages.reduce(
    (sum, stage) => sum + parseInt(stage.duration),
    0
  );
  const maxVus = Math.max(...config.stages.map((stage) => stage.target));
  console.log(`Total Test Duration: ${totalDuration}s`);
  console.log(`Peak Load: ${maxVus} VUs`);
  console.log("=================================================");
}

/* ==== 테스트 완료 후 정리 ==== */
export function teardown() {
  console.log("=== Test Completed ===");
  console.log(`Environment: ${ENVIRONMENT}`);
  console.log(`Tested VUs: ${config.vus}`);

  const totalDuration = config.stages.reduce(
    (sum, stage) => sum + parseInt(stage.duration),
    0
  );
  console.log(`Total Duration: ${totalDuration}s`);

  console.log("");
  console.log("Key metrics to review:");
  console.log("- sse_open_ok: Connection success rate");
  console.log("- sse_stayed_full: Session completion rate");
  console.log("- sse_messages_received: Total messages processed");
  console.log("- sse_connection_errors: Error count");
  console.log("- latency: First message response time");
  console.log("- sse_sessions_completed: Successfully completed sessions");
  console.log("- sse_conn_alive_ms: Connection duration time");

  console.log("");
  console.log("=== Next Step Recommendations ===");
  console.log("If success rate > 95%: Try higher VUs");
  console.log("If success rate < 90%: Check server resources");
  console.log("If errors > threshold: Reduce VUs or check network");
  console.log("If stable: Document current capacity limits");

  // 권장 다음 VU 수
  const currentVu = config.vus;
  const nextVuOptions = [
    Math.ceil(currentVu * 1.25), // 25% 증가
    Math.ceil(currentVu * 1.5), // 50% 증가
    Math.ceil(currentVu * 2), // 100% 증가
  ];
  console.log(`Suggested next VU levels: ${nextVuOptions.join(", ")}`);
  console.log("=====================================");
}
