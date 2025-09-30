import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";
import exec from "k6/execution";

// 커스텀 메트릭 정의
const sseConnections = new Counter("sse_connections_total");
const sseConnectionSuccess = new Rate("sse_connection_success_rate");
const sseMessageReceived = new Counter("sse_messages_received");
const broadcastLatency = new Trend("broadcast_latency");
const activeConnections = new Gauge("sse_active_connections");
const broadcastApiSuccess = new Rate("broadcast_api_success_rate");

// K6 테스트 실행 옵션 설정
export const options = {
  scenarios: {
    sse_connections: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 5000 },
        { duration: "45s", target: 15000 },
        { duration: "30s", target: 20000 },
        { duration: "90s", target: 20000 },
      ],
      tags: { scenario: "sse_connections" },
    },
    broadcast_sender: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      startTime: "2m30s",
      tags: { scenario: "broadcast_sender" },
    },
  },
  batch: 15,
  batchPerHost: 10,
  discardResponseBodies: true,
  noConnectionReuse: false,
  thresholds: {
    sse_connection_success_rate: ["rate>=1.00"],
    broadcast_latency: ["p(95)<8000"],
    http_req_failed: ["rate<0.15"],
    sse_messages_received: ["count>=40000"],
    broadcast_api_success_rate: ["rate>0.80"],
  },
};

// 서버 설정 (수정됨)
const BASE_URL = __ENV.BASE_URL || "https://api.tickle.kr"; // 수정: URL 오타 수정
const LOGIN_USERNAME = __ENV.LOGIN_USERNAME || "admin";
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || "password";
const LOGIN_ENDPOINT = `${BASE_URL}/api/v1/sign-in`; // 수정: 올바른 엔드포인트
const SSE_ENDPOINT = `${BASE_URL}/api/v1/notifications/connect`;
const BROADCAST_ENDPOINT = `${BASE_URL}/test/notification-event/partner`;

// 시간 추적용 전역 변수
let testStartTime = 0;
let sseConnectionStartTime = 0;
let sseConnectionCompleteTime = 0;
let broadcastStartTime = 0;
let broadcastCompleteTime = 0;

// 테스트 시작 전 JWT 토큰 획득
export function setup() {
  testStartTime = Date.now();

  console.log("=== 브로드캐스트 테스트 시작 ===");
  console.log(`시작 시간: ${new Date(testStartTime).toISOString()}`);
  console.log(`Target: ${BASE_URL}`);

  // JWT 토큰 획득을 위한 로그인
  console.log("=== JWT 토큰 획득 중... ===");

  // 수정: email 필드 사용
  const loginPayload = {
    email: LOGIN_USERNAME, // 수정: username -> email
    password: LOGIN_PASSWORD,
  };

  const loginParams = {
    headers: {
      "Content-Type": "application/json",
    },
    timeout: "30s",
  };

  let authToken;

  try {
    console.log(`로그인 API 호출: ${LOGIN_ENDPOINT}`);
    console.log(`로그인 사용자: ${LOGIN_USERNAME}`);

    // 로그인 API 호출
    const loginResponse = http.post(
      LOGIN_ENDPOINT,
      JSON.stringify(loginPayload),
      loginParams
    );

    console.log(`로그인 응답 상태: ${loginResponse.status}`);

    // 로그인 응답 확인
    const loginSuccess = check(loginResponse, {
      "로그인 성공": (r) => r.status === 200,
      "Authorization 헤더 존재": (r) =>
        r.headers["Authorization"] || r.headers["authorization"],
    });

    if (!loginSuccess) {
      console.error(`로그인 실패! 상태: ${loginResponse.status}`);
      console.error(`응답: ${loginResponse.body}`);

      // 디버깅을 위한 헤더 정보 출력
      console.error("=== 응답 헤더 정보 ===");
      Object.keys(loginResponse.headers).forEach((key) => {
        console.error(`${key}: ${loginResponse.headers[key]}`);
      });

      throw new Error("로그인 실패");
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    const authHeader =
      loginResponse.headers["Authorization"] ||
      loginResponse.headers["authorization"];

    if (!authHeader) {
      console.error("Authorization 헤더를 찾을 수 없습니다!");
      console.error("=== 모든 응답 헤더 ===");
      Object.keys(loginResponse.headers).forEach((key) => {
        console.error(`${key}: ${loginResponse.headers[key]}`);
      });
      throw new Error("Authorization 헤더 누락");
    }

    // "Bearer " 접두사 제거하여 순수 토큰만 추출
    authToken = authHeader.replace(/^Bearer\s+/i, "").trim();

    if (!authToken || authToken === authHeader) {
      console.error("Bearer 토큰 형식이 올바르지 않습니다!");
      console.error(`Authorization 헤더 값: ${authHeader}`);
      throw new Error("Bearer 토큰 추출 실패");
    }

    console.log("JWT 토큰 획득 성공!"); // 이모티콘 제거
    console.log(`Authorization 헤더: ${authHeader.substring(0, 30)}...`);
    console.log(`추출된 토큰 길이: ${authToken.length} 문자`);
    console.log(`토큰 앞부분: ${authToken.substring(0, 20)}...`);
  } catch (error) {
    console.error(`로그인 중 오류 발생: ${error.message}`);
    throw error;
  }

  // 토큰 유효성 검증 (선택사항)
  console.log("=== 토큰 유효성 검증 ===");
  const tokenTestResponse = http.get(`${BASE_URL}/api/v1/user/profile`, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
    timeout: "10s",
  });

  if (tokenTestResponse.status === 200) {
    console.log("토큰 유효성 검증 성공!"); // 이모티콘 제거
  } else if (tokenTestResponse.status === 401) {
    console.log(
      "토큰은 인식되지만 권한이 없을 수 있습니다. 테스트를 계속합니다."
    ); // 이모티콘 제거
  } else {
    console.log(`토큰 검증 응답: ${tokenTestResponse.status}`); // 이모티콘 제거
  }

  console.log(`예상 시간: 5분`);
  console.log(`목표 연결: 20,000개`);
  console.log("==================================");

  // 모든 VU가 사용할 수 있도록 토큰 반환
  return {
    startTime: testStartTime,
    authToken: authToken,
  };
}

// K6 메인 함수: setup에서 반환된 데이터를 받음
export default function (data) {
  const TOKEN = data.authToken;

  if (!TOKEN) {
    throw new Error("JWT 토큰이 setup에서 제공되지 않았습니다.");
  }

  if (exec.scenario.name === "sse_connections") {
    testSSEConnection(TOKEN);
  } else if (exec.scenario.name === "broadcast_sender") {
    sendBroadcastMessage(TOKEN);
  }
}

// SSE 연결을 생성하고 유지하는 함수
function testSSEConnection(token) {
  // SSE 연결 시작 시간 기록 (첫 번째 VU만)
  if (__VU === 1 && sseConnectionStartTime === 0) {
    sseConnectionStartTime = Date.now();
    console.log(
      `SSE 연결 시작: ${new Date(sseConnectionStartTime).toISOString()}`
    );
  }

  const startTime = Date.now();
  let connectionEstablished = false;
  let messagesReceived = 0;
  let isConnected = false;

  // 성능 향상을 위한 로깅 최적화: 1000명 중 1명만 로그 출력
  if (__VU % 1000 === 1) {
    console.log(
      `VU ${__VU}: SSE 연결 시도 (토큰: ${token.substring(0, 10)}...)`
    );
  }

  // SSE 연결을 위한 HTTP 요청 파라미터 설정
  const params = {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      "Cache-Control": "no-cache",
      Authorization: `Bearer ${token}`, // setup에서 받은 토큰 사용
    },
    timeout: "0", // 무제한 타임아웃 (VU 종료 시까지 유지)
  };

  try {
    // SSE 연결 시작 및 이벤트 핸들러 설정
    sse.open(SSE_ENDPOINT, params, function (client) {
      // SSE 연결 성공 시 실행되는 핸들러
      client.on("open", function () {
        connectionEstablished = true;
        isConnected = true;

        // Gauge 메트릭으로 활성 연결 수 증가
        activeConnections.add(1);

        // SSE 연결 완료 시간 업데이트 (마지막 연결까지)
        sseConnectionCompleteTime = Date.now();

        // 성능을 위한 제한적 로깅
        if (__VU % 1000 === 1) {
          console.log(`VU ${__VU}: 연결 성공`);
        }

        // 메트릭 업데이트
        sseConnections.add(1);
        sseConnectionSuccess.add(1);
      });

      function handleSSEMessage(event) {
        messagesReceived++;
        const receiveTime = Date.now();

        // 첫 번째 메시지 수신 시에만 로그 출력
        if (messagesReceived === 1) {
          console.log(
            `VU ${__VU}: 메시지 수신! 타입: ${event.type || "unknown"}`
          );
        }

        // 브로드캐스트 메시지의 지연시간 측정
        try {
          const messageData = JSON.parse(event.data || "{}");

          // createdAt 필드를 사용한 지연시간 계산
          if (messageData.createdAt) {
            const createdAtMs = new Date(messageData.createdAt).getTime();
            const latency = receiveTime - createdAtMs;

            // 음수 지연시간 방지 (시계 동기화 이슈 대응)
            if (latency >= 0 && latency < 60000) {
              broadcastLatency.add(latency);

              // 디버깅을 위한 제한적 로깅 (처음 3개 메시지만)
              if (messagesReceived <= 3 && __VU % 5000 === 1) {
                console.log(`VU ${__VU}: Latency: ${latency}ms`);
              }
            }
          } else {
            // createdAt이 없는 경우를 위한 디버깅 로그
            if (messagesReceived <= 3 && __VU % 5000 === 1) {
              console.log(`VU ${__VU}: No createdAt found in message`);
            }
          }
        } catch (e) {
          // JSON 파싱 실패 시 디버깅 로그
          if (messagesReceived <= 3 && __VU % 5000 === 1) {
            console.log(`VU ${__VU}: 파싱 실패: ${e.message}`);
          }
        }

        sseMessageReceived.add(1);
      }

      client.on("message", handleSSEMessage);
      client.on("notification", handleSSEMessage);
      client.on("sse-connect", handleSSEMessage);
      client.on("data", handleSSEMessage);
      client.on("event", handleSSEMessage);

      try {
        if (client.onmessage !== undefined) {
          client.onmessage = handleSSEMessage;
        }
      } catch (e) {}

      // SSE 연결 오류 시 실행되는 핸들러
      client.on("error", function (error) {
        if (__VU % 1000 === 1) {
          console.log(`VU ${__VU}: SSE 오류: ${error}`);
        }
        if (isConnected) {
          activeConnections.add(-1); // 활성 연결 수 감소
          isConnected = false;
        }
        sseConnectionSuccess.add(0);
      });

      // SSE 연결 종료 시 실행되는 핸들러
      client.on("close", function () {
        if (isConnected) {
          activeConnections.add(-1); // 활성 연결 수 감소
          isConnected = false;
        }
      });
    });
  } catch (error) {
    // SSE 연결 생성 실패 시 메트릭 업데이트
    sseConnectionSuccess.add(0);
    return;
  }

  // SSE 연결을 3분간 유지
  sleep(180);
}

// 브로드캐스트 메시지를 전송하고 결과를 측정하는 함수
function sendBroadcastMessage(token) {
  broadcastStartTime = Date.now();

  console.log("브로드캐스트 시작!");
  console.log(
    `브로드캐스트 시작 시간: ${new Date(broadcastStartTime).toISOString()}`
  );

  // SSE 연결 안정화를 위한 짧은 대기
  sleep(10);

  const apiStartTime = Date.now();
  console.log(`브로드캐스트 API 호출: ${new Date(apiStartTime).toISOString()}`);

  let response;
  try {
    // 브로드캐스트 API 호출
    response = http.get(BROADCAST_ENDPOINT, {
      headers: {
        Authorization: `Bearer ${token}`, // setup에서 받은 토큰 사용
      },
      timeout: "60s", // 1분 타임아웃 설정
    });
  } catch (error) {
    console.log(`HTTP 요청 실패: ${error.message}`);
    return;
  }

  // 브로드캐스트 API 응답 분석
  const duration = Date.now() - apiStartTime;
  const success = response.status === 200;

  // 테스트 결과 출력
  console.log("==================================================");
  console.log("브로드캐스트 결과");
  console.log(`완료 시간: ${new Date().toISOString()}`);
  console.log(`소요 시간: ${duration}ms (${(duration / 1000).toFixed(2)}초)`);
  console.log(`HTTP 상태: ${response.status}`);

  if (success) {
    console.log("브로드캐스트 성공!");
    console.log(`예상 처리량: ${Math.round(50000 / (duration / 1000))}개/초`);
  } else {
    console.log("브로드캐스트 실패");
    console.log(`응답: ${response.body?.substring(0, 100)}...`);
  }
  console.log("==================================================");

  // 브로드캐스트 API 성공/실패 메트릭 업데이트
  broadcastApiSuccess.add(success ? 1 : 0);

  if (success) {
    // 성공 시 메시지 전파 완료를 위한 대기
    console.log("메시지 전파 대기 (30초)...");
    sleep(30);

    broadcastCompleteTime = Date.now();
    console.log("브로드캐스트 테스트 완료!");
    console.log(
      `브로드캐스트 완료 시간: ${new Date(broadcastCompleteTime).toISOString()}`
    );
  } else {
    broadcastCompleteTime = Date.now(); // 실패해도 완료 시간 기록
    console.log("브로드캐스트 실패로 테스트 종료");
  }
}
