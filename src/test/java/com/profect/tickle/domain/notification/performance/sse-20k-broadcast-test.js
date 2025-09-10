import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";

// 사용자 정의 메트릭
const sseConnections = new Counter("sse_connections_total");
const sseConnectionSuccess = new Rate("sse_connection_success_rate");
const sseMessageReceived = new Counter("sse_messages_received");
const sseConnectionDuration = new Trend("sse_connection_duration");
const broadcastLatency = new Trend("broadcast_latency");
const activeConnections = new Gauge("sse_active_connections");

// 테스트 옵션 설정
export const options = {
  scenarios: {
    sse_connections: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 5000 },
        { duration: "1m", target: 10000 },
        { duration: "1m", target: 15000 },
        { duration: "1m", target: 20000 },
        { duration: "6m", target: 20000 },
      ],
      tags: { scenario: "sse_connections" },
    },
    broadcast_sender: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      startTime: "4m",
      tags: { scenario: "broadcast_sender" },
    },
  },
  batch: 20,
  batchPerHost: 10,
  discardResponseBodies: true,
  thresholds: {
    sse_connection_success_rate: ["rate>0.95"],
    sse_connection_duration: ["p(95)<5000"],
    broadcast_latency: ["p(95)<2000"],
    http_req_failed: ["rate<0.05"],
    sse_active_connections: ["value>=19000"],
    sse_messages_received: ["count>=19000"],
  },
};

// 환경 설정
const BASE_URL = __ENV.BASE_URL || "http://172.16.24.202:8081";
const TOKEN = __ENV.TOKEN;
const SSE_ENDPOINT = `${BASE_URL}/api/v1/notifications/connect`;
const BROADCAST_ENDPOINT = `${BASE_URL}/test/notification-event/partner`;

export default function () {
  if (__SCENARIO === "sse_connections") {
    testSSEConnection();
  } else if (__SCENARIO === "broadcast_sender") {
    sendBroadcastMessage();
  }
}

// SSE 연결 테스트 함수
function testSSEConnection() {
  const startTime = Date.now();
  let connectionEstablished = false;
  let messagesReceived = 0;
  let broadcastMessageReceived = false;
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
    timeout: "10s",
  };

  try {
    const response = sse.open(SSE_ENDPOINT, params, function (client) {
      client.on("open", function () {
        connectionEstablished = true;
        isConnected = true;
        const connectionTime = Date.now() - startTime;

        console.log(`VU ${__VU}: SSE 연결 성공 (${connectionTime}ms)`);

        sseConnections.add(1);
        sseConnectionSuccess.add(1);
        sseConnectionDuration.add(connectionTime);
        activeConnections.add(1);
      });

      client.on("event", function (event) {
        messagesReceived++;
        const currentTime = Date.now();

        console.log(
          `VU ${__VU}: 메시지 수신 #${messagesReceived} - Type: ${
            event.type || "message"
          }, Data: ${(event.data || "").substring(0, 100)}...`
        );

        // 브로드캐스트 메시지 latency 측정
        try {
          const messageData = JSON.parse(event.data);
          if (messageData.timestamp) {
            const latency = currentTime - messageData.timestamp;
            broadcastLatency.add(latency);
            broadcastMessageReceived = true;
            console.log(
              `VU ${__VU}: 브로드캐스트 메시지 수신! 지연시간: ${latency}ms`
            );
          }
        } catch (e) {
          console.log(`VU ${__VU}: 일반 메시지 수신`);
        }

        sseMessageReceived.add(1);
      });

      client.on("error", function (error) {
        console.log(`VU ${__VU}: SSE 오류 발생: ${error.error()}`);
        if (isConnected) {
          activeConnections.add(-1);
          isConnected = false;
        }
        sseConnectionSuccess.add(0);
      });

      client.on("close", function () {
        console.log(
          `VU ${__VU}: SSE 연결 종료 (메시지 수신: ${messagesReceived}개, 브로드캐스트 수신: ${
            broadcastMessageReceived ? "YES" : "NO"
          })`
        );
        if (isConnected) {
          activeConnections.add(-1);
          isConnected = false;
        }
      });
    });

    check(response, {
      "SSE 연결 성공": (r) => r && r.status === 200,
      "SSE 연결 응답 헤더 확인": (r) =>
        r &&
        r.headers["Content-Type"] &&
        r.headers["Content-Type"].includes("text/event-stream"),
      "JWT 인증 성공": (r) => r && r.status !== 401 && r.status !== 403,
    });
  } catch (error) {
    console.log(`VU ${__VU}: SSE 연결 예외: ${error.message}`);
    sseConnectionSuccess.add(0);
  }

  // 연결 유지 (6분간)
  sleep(360);
}

// 브로드캐스트 메시지 전송 함수 (1회만 호출)
function sendBroadcastMessage() {
  console.log("=== 브로드캐스트 시작 ===");
  console.log("20,000명 SSE 연결 완료 후 브로드캐스트 1회 실행");

  // 연결 안정화를 위한 추가 대기
  sleep(10);

  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${TOKEN}`,
    },
    tags: {
      scenario: "broadcast",
    },
  };

  console.log("브로드캐스트 API 호출 중...");
  const startTime = Date.now();

  // GET 요청으로 브로드캐스트 트리거
  const response = http.get(BROADCAST_ENDPOINT, params);

  const requestDuration = Date.now() - startTime;

  check(response, {
    "브로드캐스트 API 호출 성공": (r) => r.status === 200,
    "브로드캐스트 응답 시간 < 1초": (r) => r.timings.duration < 1000,
    "JWT 인증 성공": (r) => r.status !== 401 && r.status !== 403,
  });

  if (response.status === 200) {
    console.log(`브로드캐스트 API 호출 성공: ${requestDuration}ms`);

    try {
      const responseBody = JSON.parse(response.body);
      if (responseBody.sentCount) {
        console.log(`브로드캐스트 전송 대상: ${responseBody.sentCount}명`);
      }
      if (responseBody.message) {
        console.log(`서버 응답: ${responseBody.message}`);
      }
    } catch (e) {
      console.log("브로드캐스트 API 호출 완료");
    }

    console.log("SSE 클라이언트들의 메시지 수신을 대기 중...");
  } else {
    console.log(
      `브로드캐스트 API 호출 실패: ${response.status} - ${response.body}`
    );
  }

  // 브로드캐스트 후 메시지 전파 시간을 위해 잠시 대기
  sleep(30);
  console.log("=== 브로드캐스트 완료 ===");
}

export function setup() {
  console.log("=== SSE 브로드캐스트 부하 테스트 시작 ===");
  console.log(`Target URL: ${BASE_URL}`);
  console.log(`SSE Endpoint: ${SSE_ENDPOINT}`);
  console.log(`Broadcast Endpoint: ${BROADCAST_ENDPOINT}`);
  console.log(`목표 SSE 연결 수: 20,000`);
  console.log(`브로드캐스트 시작 시간: 4분 후 (연결 안정화 대기)`);

  // TOKEN 확인
  if (!TOKEN) {
    throw new Error("TOKEN 환경 변수가 설정되지 않았습니다.");
  }
  console.log(`TOKEN 길이: ${TOKEN.length}자`);

  // 서버 연결 테스트 (선택적)
  try {
    const testResponse = http.get(`${BASE_URL}/health`, {
      headers: { Authorization: `Bearer ${TOKEN}` },
      timeout: "5s",
    });
    console.log(`서버 연결 테스트: ${testResponse.status}`);
  } catch (e) {
    console.log(`서버 연결 테스트 실패: ${e.message} (계속 진행)`);
  }

  return { startTime: Date.now() };
}

export function teardown(data) {
  const endTime = Date.now();
  const totalDuration = (endTime - data.startTime) / 1000;

  console.log("=== SSE 브로드캐스트 부하 테스트 완료 ===");
  console.log(`총 테스트 시간: ${totalDuration}초`);
  console.log("=== 최종 통계 ===");
  console.log("- SSE 연결 성공률과 메시지 수신 통계는 위의 요약을 참고하세요");
}
