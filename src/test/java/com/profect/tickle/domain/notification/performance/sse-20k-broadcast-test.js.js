import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";

// 사용자 정의 메트릭
const sseConnections = new Counter("sse_connections_total");
const sseConnectionSuccess = new Rate("sse_connection_success_rate");
const sseMessageReceived = new Counter("sse_messages_received");
const sseConnectionDuration = new Trend("sse_connection_duration");
const broadcastLatency = new Trend("broadcast_latency");
const activeConnections = new Gauge("sse_active_connections"); // 현재 활성 연결 수

// 테스트 옵션 설정
export const options = {
  scenarios: {
    sse_connections: {
      executor: "ramping-vus", // 점진적 연결 증가로 변경
      startVUs: 0,
      stages: [
        { duration: "1m", target: 5000 }, // 1분에 걸쳐 5,000명
        { duration: "1m", target: 10000 }, // 1분에 걸쳐 10,000명
        { duration: "1m", target: 15000 }, // 1분에 걸쳐 15,000명
        { duration: "1m", target: 20000 }, // 1분에 걸쳐 20,000명
        { duration: "6m", target: 20000 }, // 6분간 20,000명 유지
      ],
      tags: { scenario: "sse_connections" },
    },
    broadcast_sender: {
      executor: "constant-vus",
      vus: 1,
      duration: "4m", // 브로드캐스트 지속 시간
      startTime: "4m", // SSE 연결이 완전히 안정화된 후 시작 (4분 후)
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
    sse_active_connections: ["value>=20000"],
  },
};

// 환경 설정
const BASE_URL = __ENV.BASE_URL || "http://172.16.24.202:8081";
const JWT_TOKEN = __ENV.JWT_TOKEN;
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
  let isConnected = false;

  console.log(`VU ${__VU}: SSE 연결 시도 중...`);

  const params = {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
      "User-Agent": `k6-sse-test-vu-${__VU}`,
      Authorization: `Bearer ${JWT_TOKEN}`,
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
        activeConnections.add(1); // 활성 연결 수 증가
      });

      client.on("event", function (event) {
        messagesReceived++;
        const currentTime = Date.now();

        console.log(
          `VU ${__VU}: 메시지 수신 #${messagesReceived} - Type: ${
            event.type || "unknown"
          }, Data: ${event.data}`
        );

        // 브로드캐스트 메시지 latency 측정
        try {
          const messageData = JSON.parse(event.data);
          if (messageData.timestamp) {
            const latency = currentTime - messageData.timestamp;
            broadcastLatency.add(latency);
            console.log(`VU ${__VU}: 브로드캐스트 지연시간: ${latency}ms`);
          }
        } catch (e) {
          // JSON이 아닌 메시지일 수도 있으므로 에러 로그는 debug 레벨로만
          if (__ENV.DEBUG) {
            console.log(`VU ${__VU}: 메시지 파싱 실패: ${e.message}`);
          }
        }

        sseMessageReceived.add(1);
      });

      client.on("error", function (error) {
        console.log(`VU ${__VU}: SSE 오류 발생: ${error.error()}`);
        if (isConnected) {
          activeConnections.add(-1); // 활성 연결 수 감소
          isConnected = false;
        }
        sseConnectionSuccess.add(0);
      });

      client.on("close", function () {
        console.log(`VU ${__VU}: SSE 연결 종료`);
        if (isConnected) {
          activeConnections.add(-1); // 활성 연결 수 감소
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

// 브로드캐스트 메시지 전송 함수
function sendBroadcastMessage() {
  // 첫 번째 브로드캐스트 전에 연결 상태 확인
  if (__ITER === 0) {
    console.log("=== 브로드캐스트 시작 ===");
    console.log("20,000명 SSE 연결 완료 후 브로드캐스트 시작");
    sleep(5); // 초기 대기
  }

  const messagePayload = {
    type: "broadcast",
    message: `브로드캐스트 메시지 #${__ITER + 1} - ${Date.now()}`,
    timestamp: Date.now(),
    target: "all",
    iteration: __ITER + 1,
  };

  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${JWT_TOKEN}`,
    },
    tags: {
      scenario: "broadcast",
      iteration: __ITER + 1,
    },
  };

  console.log(`브로드캐스트 메시지 #${__ITER + 1} 전송 중...`);

  const response = http.post(
    BROADCAST_ENDPOINT,
    JSON.stringify(messagePayload),
    params
  );

  check(response, {
    "브로드캐스트 전송 성공": (r) => r.status === 200,
    "브로드캐스트 응답 시간 < 1초": (r) => r.timings.duration < 1000,
    "JWT 인증 성공": (r) => r.status !== 401 && r.status !== 403,
  });

  if (response.status === 200) {
    console.log(
      `브로드캐스트 #${__ITER + 1} 전송 완료: ${response.timings.duration}ms`
    );

    // 응답 본문에서 전송된 클라이언트 수 확인 (서버가 제공하는 경우)
    try {
      const responseBody = JSON.parse(response.body);
      if (responseBody.sentCount) {
        console.log(
          `브로드캐스트 #${__ITER + 1}: ${responseBody.sentCount}명에게 전송됨`
        );
      }
    } catch (e) {
      // 응답이 JSON이 아닐 수 있음
    }
  } else {
    console.log(
      `브로드캐스트 #${__ITER + 1} 전송 실패: ${response.status} - ${
        response.body
      }`
    );
  }

  // 30초마다 브로드캐스트 전송
  sleep(30);
}

export function setup() {
  console.log("=== SSE 브로드캐스트 부하 테스트 시작 ===");
  console.log(`Target URL: ${BASE_URL}`);
  console.log(`SSE Endpoint: ${SSE_ENDPOINT}`);
  console.log(`Broadcast Endpoint: ${BROADCAST_ENDPOINT}`);
  console.log(`목표 SSE 연결 수: 20,000`);
  console.log(`브로드캐스트 시작 시간: 4분 후 (연결 안정화 대기)`);

  // JWT 토큰 확인
  if (!JWT_TOKEN) {
    throw new Error("JWT_TOKEN 환경 변수가 설정되지 않았습니다.");
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
