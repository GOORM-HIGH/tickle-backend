import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";
import exec from "k6/execution";

// 커스텀 메트릭 정의
const sseConnections = new Counter("sse_connections_total"); // 총 SSE 연결 생성 횟수
const sseConnectionSuccess = new Rate("sse_connection_success_rate"); // SSE 연결 성공률
const sseMessageReceived = new Counter("sse_messages_received"); // 수신된 SSE 메시지 총 개수
const broadcastLatency = new Trend("broadcast_latency"); // 브로드캐스트 메시지 지연시간 분포
const activeConnections = new Gauge("sse_active_connections"); // 현재 활성 SSE 연결 수
const broadcastApiSuccess = new Rate("broadcast_api_success_rate"); // 브로드캐스트 API 호출 성공률

// K6 테스트 실행 옵션 설정 - 총 5분 동안 실행되는 최적화된 테스트
export const options = {
  scenarios: {
    // 시나리오 1: SSE 연결 생성 및 유지 (20,000개 클라이언트 시뮬레이션)
    sse_connections: {
      executor: "ramping-vus", // 점진적으로 가상 사용자 수 증가
      startVUs: 0, // 시작 시 가상 사용자 수
      stages: [
        { duration: "30s", target: 5000 }, // 30초간 5,000개 연결로 램프업
        { duration: "45s", target: 15000 }, // 45초간 15,000개 연결로 증가
        { duration: "30s", target: 20000 }, // 30초간 20,000개 연결로 최종 증가
        { duration: "90s", target: 20000 }, // 90초간 20,000개 연결 유지
      ],
      tags: { scenario: "sse_connections" }, // 시나리오 태그 설정
    },
    // 시나리오 2: 브로드캐스트 메시지 전송 (1개 가상 사용자가 1회 실행)
    broadcast_sender: {
      executor: "per-vu-iterations", // VU당 반복 실행 방식
      vus: 1, // 1개 가상 사용자
      iterations: 1, // 1회 실행
      startTime: "2m30s", // 2분 30초 후 실행 시작 (연결 안정화 대기)
      tags: { scenario: "broadcast_sender" }, // 시나리오 태그 설정
    },
  },

  // K6 성능 최적화 설정
  batch: 15, // HTTP 요청 배치 처리 크기
  batchPerHost: 10, // 호스트별 배치 처리 크기
  discardResponseBodies: true, // 응답 본문 폐기로 메모리 절약
  noConnectionReuse: false, // HTTP 연결 재사용 활성화

  // 임계값 설정
  thresholds: {
    sse_connection_success_rate: ["rate>=1.00"], // SSE 연결 성공률 100% 이상
    broadcast_latency: ["p(95)<8000"], // 브로드캐스트 지연시간 95분위수 8초 미만
    http_req_failed: ["rate<0.15"], // HTTP 요청 실패율 15% 미만
    sse_messages_received: ["count>=40000"], // 수신된 메시지 = 40,000개
    broadcast_api_success_rate: ["rate>0.80"], // 브로드캐스트 API 성공률 80% 이상
  },
};

// 테스트 대상 서버 및 엔드포인트 설정
const BASE_URL = __ENV.BASE_URL || "http://172.16.24.202:8081";
const TOKEN = __ENV.TOKEN;
const SSE_ENDPOINT = `${BASE_URL}/api/v1/notifications/connect`; // SSE 연결 엔드포인트
const BROADCAST_ENDPOINT = `${BASE_URL}/test/notification-event/partner`; // 브로드캐스트 API 엔드포인트

// 전역 변수: 현재 활성 연결 수 추적용 (동시성 문제 있을 수 있음)
let globalActiveConnections = 0;

// 시간 추적용 전역 변수 추가
let testStartTime = 0;
let sseConnectionStartTime = 0;
let sseConnectionCompleteTime = 0;
let broadcastStartTime = 0;
let broadcastCompleteTime = 0;

// K6 메인 함수: 시나리오에 따라 다른 함수 실행
export default function () {
  if (exec.scenario.name === "sse_connections") {
    testSSEConnection(); // SSE 연결 테스트
  } else if (exec.scenario.name === "broadcast_sender") {
    sendBroadcastMessage(); // 브로드캐스트 전송 테스트
  }
}

// SSE 연결을 생성하고 유지하는 함수
function testSSEConnection() {
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
    console.log(`VU ${__VU}: SSE 연결 시도`);
  }

  // SSE 연결을 위한 HTTP 요청 파라미터 설정
  const params = {
    method: "GET",
    headers: {
      Accept: "text/event-stream", // SSE 표준 미디어 타입
      "Cache-Control": "no-cache", // 캐시 방지
      Authorization: `Bearer ${TOKEN}`, // 인증 토큰
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
        globalActiveConnections++;

        // SSE 연결 완료 시간 업데이트 (마지막 연결까지)
        sseConnectionCompleteTime = Date.now();

        // 성능을 위한 제한적 로깅
        if (__VU % 1000 === 1) {
          console.log(
            `VU ${__VU}: 연결 성공 - 총 ${globalActiveConnections}개`
          );
        }

        // 메트릭 업데이트
        sseConnections.add(1);
        sseConnectionSuccess.add(1);
        activeConnections.add(globalActiveConnections);
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

        // 브로드캐스트 메시지의 지연시간 측정 (NotificationEnvelope의 createdAt 활용)
        try {
          const messageData = JSON.parse(event.data || "{}");

          // createdAt 필드를 사용한 지연시간 계산
          if (messageData.createdAt) {
            // ISO 8601 문자열을 밀리초로 변환
            const createdAtMs = new Date(messageData.createdAt).getTime();
            const latency = receiveTime - createdAtMs;

            // 음수 지연시간 방지 (시계 동기화 이슈 대응)
            if (latency >= 0 && latency < 60000) {
              // 60초 이내의 합리적인 지연시간만 측정
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
      } catch (e) {
        // 지원하지 않으면 무시
      }

      // SSE 연결 오류 시 실행되는 핸들러
      client.on("error", function (error) {
        if (__VU % 1000 === 1) {
          console.log(`VU ${__VU}: SSE 오류: ${error}`);
        }
        if (isConnected) {
          globalActiveConnections--;
          isConnected = false;
        }
        sseConnectionSuccess.add(0);
      });

      // SSE 연결 종료 시 실행되는 핸들러
      client.on("close", function () {
        if (isConnected) {
          globalActiveConnections--;
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
function sendBroadcastMessage() {
  broadcastStartTime = Date.now();

  console.log("브로드캐스트 시작!");
  console.log(
    `브로드캐스트 시작 시간: ${new Date(broadcastStartTime).toISOString()}`
  );
  console.log(`현재 활성 연결: ~${globalActiveConnections}개`);

  // SSE 연결 안정화를 위한 짧은 대기
  sleep(10);

  const apiStartTime = Date.now();
  console.log(`브로드캐스트 API 호출: ${new Date(apiStartTime).toISOString()}`);

  let response;
  try {
    // 브로드캐스트 API 호출
    response = http.get(BROADCAST_ENDPOINT, {
      headers: {
        Authorization: `Bearer ${TOKEN}`,
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

// 테스트 시작 전 초기화 함수
export function setup() {
  testStartTime = Date.now();

  console.log("=== 브로드캐스트 테스트 시작 ===");
  console.log(`시작 시간: ${new Date(testStartTime).toISOString()}`);
  console.log(`Target: ${BASE_URL}`);
  console.log(`예상 시간: 5분`);
  console.log(`목표 연결: 20,000개`);

  // TOKEN 환경변수 필수 확인
  if (!TOKEN) {
    throw new Error("TOKEN 환경 변수 필요");
  }

  return { startTime: testStartTime };
}
