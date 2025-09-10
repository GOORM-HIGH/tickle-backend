import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";
import exec from "k6/execution";

// 사용자 정의 메트릭
const sseConnections = new Counter("sse_connections_total");
const sseConnectionSuccess = new Rate("sse_connection_success_rate");
const sseMessageReceived = new Counter("sse_messages_received");
const sseConnectionDuration = new Trend("sse_connection_duration");
const broadcastLatency = new Trend("broadcast_latency");
const activeConnections = new Gauge("sse_active_connections");
const broadcastApiSuccess = new Rate("broadcast_api_success_rate"); // 추가

// 테스트 옵션 수정
export const options = {
  scenarios: {
    sse_connections: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 5000 },
        { duration: "2m", target: 10000 },
        { duration: "2m", target: 15000 },
        { duration: "2m", target: 20000 },
        { duration: "8m", target: 20000 },
      ],
      tags: { scenario: "sse_connections" },
    },
    broadcast_sender: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      startTime: "6m",
      tags: { scenario: "broadcast_sender" },
    },
  },
  batch: 10,
  batchPerHost: 5,
  discardResponseBodies: true,

  // 임계값 완화
  thresholds: {
    sse_connection_success_rate: ["rate>0.80"],
    sse_connection_duration: ["p(95)<10000"],
    broadcast_latency: ["p(95)<5000"],
    http_req_failed: ["rate<0.10"],
    sse_active_connections: ["value>=16000"],
    sse_messages_received: ["count>=16000"],
    broadcast_api_success_rate: ["rate>0.80"],
  },
};

// 환경 설정
const BASE_URL = __ENV.BASE_URL || "http://172.16.24.202:8081";
const TOKEN = __ENV.TOKEN;
const SSE_ENDPOINT = `${BASE_URL}/api/v1/notifications/connect`;
const BROADCAST_ENDPOINT = `${BASE_URL}/test/notification-event/partner`;

// 전역 연결 카운터 (더 정확한 추적)
let globalActiveConnections = 0;

export default function () {
  if (exec.scenario.name === "sse_connections") {
    testSSEConnection();
  } else if (exec.scenario.name === "broadcast_sender") {
    sendBroadcastMessage();
  }
}

// SSE 연결 테스트 함수
function testSSEConnection() {
  const startTime = Date.now();
  let connectionEstablished = false;
  let messagesReceived = 0;
  let isConnected = false;

  console.log(`VU ${__VU}: SSE 연결 시도 중...`);

  const params = {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
      "User-Agent": `k6-sse-test-vu-${__VU}`,
      Authorization: `Bearer ${TOKEN}`,
    },
    tags: {
      vu_id: __VU,
      scenario: "sse_connection",
    },
    timeout: "30s", // 타임아웃 증가
  };

  try {
    const response = sse.open(SSE_ENDPOINT, params, function (client) {
      client.on("open", function () {
        connectionEstablished = true;
        isConnected = true;
        globalActiveConnections++;
        const connectionTime = Date.now() - startTime;

        console.log(
          `VU ${__VU}: SSE 연결 성공 (${connectionTime}ms) - 총 연결: ${globalActiveConnections}`
        );

        sseConnections.add(1);
        sseConnectionSuccess.add(1);
        sseConnectionDuration.add(connectionTime);
        activeConnections.add(globalActiveConnections);
      });

      client.on("event", function (event) {
        messagesReceived++;
        const currentTime = Date.now();

        // 안전한 문자열 처리
        const eventData = event.data || "";
        const displayData =
          eventData.length > 50
            ? eventData.substring(0, 50) + "..."
            : eventData;

        console.log(
          `VU ${__VU}: 메시지 수신 #${messagesReceived} - Data: ${displayData}`
        );

        // 브로드캐스트 메시지 latency 측정
        try {
          const messageData = JSON.parse(eventData);
          if (messageData.timestamp) {
            const latency = currentTime - messageData.timestamp;
            broadcastLatency.add(latency);
            console.log(`VU ${__VU}: 브로드캐스트 지연시간: ${latency}ms`);
          }
        } catch (e) {
          // JSON이 아닌 메시지도 정상 처리
        }

        sseMessageReceived.add(1);
      });

      client.on("error", function (error) {
        console.log(`VU ${__VU}: SSE 오류 - ${error.error()}`);
        if (isConnected) {
          globalActiveConnections--;
          activeConnections.add(globalActiveConnections);
          isConnected = false;
        }
        sseConnectionSuccess.add(0);
      });

      client.on("close", function () {
        console.log(
          `VU ${__VU}: SSE 연결 종료 (메시지: ${messagesReceived}개)`
        );
        if (isConnected) {
          globalActiveConnections--;
          activeConnections.add(globalActiveConnections);
          isConnected = false;
        }
      });
    });

    check(response, {
      "SSE 연결 성공": (r) => r && r.status === 200,
      "SSE 응답 헤더 확인": (r) =>
        r &&
        r.headers["Content-Type"] &&
        r.headers["Content-Type"].includes("text/event-stream"),
      "인증 성공": (r) => r && r.status !== 401 && r.status !== 403,
    });

    if (!response || response.status !== 200) {
      console.log(
        `VU ${__VU}: SSE 연결 실패 - Status: ${
          response?.status
        }, Headers: ${JSON.stringify(response?.headers)}`
      );
      sseConnectionSuccess.add(0);
      return;
    }
  } catch (error) {
    console.log(`VU ${__VU}: SSE 연결 예외 - ${error.message}`);
    sseConnectionSuccess.add(0);
    return;
  }

  // 연결 유지 (8분간)
  sleep(480);
}

// 브로드캐스트 함수 개선
function sendBroadcastMessage() {
  console.log("=== 브로드캐스트 시작 ===");
  console.log(`현재 활성 연결 수: ${globalActiveConnections}`);

  // 충분한 대기 시간
  sleep(30);

  // 브로드캐스트 전에 서버 상태 확인
  try {
    const healthCheck = http.get(`${BASE_URL}/health`, {
      headers: { Authorization: `Bearer ${TOKEN}` },
      timeout: "10s",
    });
    console.log(`서버 상태 확인: ${healthCheck.status}`);
  } catch (e) {
    console.log(`서버 상태 확인 실패: ${e.message}`);
  }

  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TOKEN}`,
    },
    tags: { scenario: "broadcast" },
    timeout: "30s",
  };

  console.log("브로드캐스트 API 호출...");
  console.log(`URL: ${BROADCAST_ENDPOINT}`);
  console.log(`Headers: ${JSON.stringify(params.headers)}`);

  const response = http.get(BROADCAST_ENDPOINT, params);

  console.log(
    `브로드캐스트 응답: Status=${
      response.status
    }, Body=${response.body?.substring(0, 200)}`
  );

  const success = response.status === 200;
  broadcastApiSuccess.add(success ? 1 : 0);

  check(response, {
    "브로드캐스트 API 성공": (r) => r.status === 200,
    "브로드캐스트 응답시간": (r) => r.timings.duration < 10000,
    "브로드캐스트 인증": (r) => r.status !== 401 && r.status !== 403,
  });

  if (success) {
    console.log("브로드캐스트 성공 - SSE 메시지 전파 대기 중...");
    sleep(60); // 메시지 전파 대기
  } else {
    console.log(`브로드캐스트 실패: ${response.status} - ${response.body}`);
  }
}

export function setup() {
  console.log("=== SSE 브로드캐스트 부하 테스트 시작 ===");
  console.log(`Target: ${BASE_URL}`);
  console.log(`SSE: ${SSE_ENDPOINT}`);
  console.log(`Broadcast: ${BROADCAST_ENDPOINT}`);

  if (!TOKEN) {
    throw new Error("TOKEN 환경 변수 필요");
  }

  // 사전 연결 테스트
  const testResponse = http.get(`${BASE_URL}/health`);
  console.log(`사전 테스트: ${testResponse.status}`);

  return { startTime: Date.now() };
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  console.log("=== 테스트 완료 ===");
  console.log(`총 시간: ${duration}초`);
  console.log(`최종 활성 연결: ${globalActiveConnections}`);
}
