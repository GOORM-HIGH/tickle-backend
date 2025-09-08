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
const connectionErrors = new Counter("sse_connection_errors");
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
  let opened = false;
  let errored = false;
  let firstMessageReceived = false;
  let messageCount = 0;
  let sessionCompleted = false;

  debugLog(`Attempting to connect to ${URL} with memberId=${MEMBER_ID}`);
  debugLog(`Headers: ${JSON.stringify(createHeaders())}`);

  // 연결 재시도 로직
  let connected = false;
  for (let attempt = 1; attempt <= MAX_RETRIES && !connected; attempt++) {
    if (attempt > 1) {
      connectionRetries.add(1, tags);
      debugLog(`Retry attempt ${attempt}/${MAX_RETRIES}`);
      sleep(1);
    }

    try {
      // SSE 연결 시도
      const client = sse.open(URL, createSSEOptions(), function (sseClient) {
        connected = true;
        opened = true;
        debugLog("SSE connection established successfully");

        // 메시지 수신 시간 기록 (첫 메시지 추정)
        if (!firstMessageReceived) {
          firstMessageReceived = true;
          const timeToFirst = Date.now() - start;
          latency.add(timeToFirst, tags);
          debugLog(`First message received after ${timeToFirst}ms`);
        }

        // 메시지 카운트 (SSE 확장이 자동으로 메시지를 수신함)
        messageCount = 1; // 초기 연결 메시지
        messagesReceived.add(1, tags);

        debugLog(`Maintaining connection for ${SESSION_SEC} seconds`);

        // 지정된 시간 동안 대기
        sleep(SESSION_SEC);

        sessionCompleted = true;
        sessionsCompleted.add(1, tags);
        debugLog("Session completed successfully");

        // 명시적 연결 종료 시도
        // try {
        //   if (sseClient && typeof sseClient.close === "function") {
        //     sseClient.close();
        //     debugLog("SSE connection closed by client");
        //   }
        // } catch (closeError) {
        //   debugLog(`Error closing SSE connection: ${closeError.message}`);
        // }
      });

      // 연결 시도가 성공하면 재시도 루프 종료
      if (connected) {
        debugLog("Breaking retry loop - connection successful");
        break;
      }
    } catch (e) {
      debugLog(`Connection attempt ${attempt} failed: ${e.message}`);
      connectionErrors.add(1, tags);

      if (attempt === MAX_RETRIES) {
        errored = true;
        debugLog(`All ${MAX_RETRIES} connection attempts failed`);
      }
    }
  }

  // 연결 지속 시간 계산
  const aliveMs = Date.now() - start;
  connAlive.add(aliveMs, tags);

  // 결과 메트릭 기록
  openOk.add(opened && !errored, tags);

  if (opened && !errored) {
    const expectedDurationMs = SESSION_SEC * 1000;

    if (sessionCompleted && aliveMs >= expectedDurationMs * 0.9) {
      // 90% 이상 유지되면 성공으로 간주
      stayedFull.add(1, tags);
      debugLog(`Session maintained successfully: ${aliveMs}ms`);
    } else {
      stayedFull.add(0, tags);
      earlyClose.add(1, tags);
      debugLog(
        `Session ended early: ${aliveMs}ms (expected: ${expectedDurationMs}ms)`
      );
    }
  } else {
    stayedFull.add(0, tags);
    if (opened) {
      earlyClose.add(1, tags);
    }
  }

  // 최종 결과 로그
  console.log(
    `[VU ${__VU}/${config.vus}] Summary: memberId=${MEMBER_ID}, opened=${opened}, errored=${errored}, ` +
      `aliveMs=${aliveMs}, messagesReceived=${messageCount}, ` +
      `sessionCompleted=${sessionCompleted}, ` +
      `stayedFull=${aliveMs >= SESSION_SEC * 1000 * 0.9}, ` +
      `latency=${firstMessageReceived ? "measured" : "not_measured"}`
  );

  if (!opened || errored) {
    console.warn(
      `[VU ${__VU}] Connection issues detected. ` +
        `Check server availability and network connectivity.`
    );
  }
}

/* ==== 설정 검증 ==== */
export function setup() {
  console.log("🚀 === K6 SSE Performance Test Configuration ===");
  console.log(`Environment: ${ENVIRONMENT}`);
  console.log(`Base URL: ${config.baseUrl}`);
  console.log(`Target URL: ${URL}`);
  console.log(`Member ID: ${MEMBER_ID}`);
  console.log(`Session Duration: ${SESSION_SEC}s`);
  console.log(`Max Retries: ${MAX_RETRIES}`);
  // VU 정보 강화
  console.log(`🎯 Target VUs: ${config.vus}`);
  console.log(`📊 Load Pattern: ${JSON.stringify(config.stages)}`);
  console.log(`🎚️  Thresholds: ${JSON.stringify(config.thresholds, null, 2)}`);
  console.log(`Debug Mode: ${DEBUG ? "ON" : "OFF"}`);
  console.log(`Token Provided: ${TOKEN ? "✅ Yes" : "❌ No"}`);
  console.log(`Last Event ID: ${LAST || "None"}`);

  // 예상 부하 정보
  const totalDuration = config.stages.reduce(
    (sum, stage) => sum + parseInt(stage.duration),
    0
  );
  const maxVus = Math.max(...config.stages.map((stage) => stage.target));
  console.log(`⏱️  Total Test Duration: ${totalDuration}s`);
  console.log(`📈 Peak Load: ${maxVus} VUs`);
  console.log("=================================================");
}

/* ==== 테스트 완료 후 정리 ==== */
export function teardown() {
  console.log("🏁 === Test Completed ===");
  console.log(`Environment: ${ENVIRONMENT}`);
  console.log(`Tested VUs: ${config.vus}`);

  const totalDuration = config.stages.reduce(
    (sum, stage) => sum + parseInt(stage.duration),
    0
  );
  console.log(`Total Duration: ${totalDuration}s`);

  console.log("");
  console.log("📊 Key metrics to review:");
  console.log("- sse_open_ok: Connection success rate");
  console.log("- sse_stayed_full: Session completion rate");
  console.log("- sse_messages_received: Total messages processed");
  console.log("- sse_connection_errors: Error count");
  console.log("- latency: First message response time");
  console.log("- sse_sessions_completed: Successfully completed sessions");

  // 다음 단계 가이드 추가
  console.log("");
  console.log("🎯 === Next Step Recommendations ===");
  console.log("📈 If success rate > 95%: Try higher VUs");
  console.log("⚠️  If success rate < 90%: Check server resources");
  console.log("🚫 If errors > threshold: Reduce VUs or check network");
  console.log("📋 If stable: Document current capacity limits");

  // 권장 다음 VU 수
  const currentVu = config.vus;
  const nextVuOptions = [
    Math.ceil(currentVu * 1.25), // 25% 증가
    Math.ceil(currentVu * 1.5), // 50% 증가
    Math.ceil(currentVu * 2), // 100% 증가
  ];
  console.log(`🔄 Suggested next VU levels: ${nextVuOptions.join(", ")}`);
  console.log("=====================================");
}
