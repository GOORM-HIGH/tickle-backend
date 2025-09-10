import sse from "k6/x/sse";
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import exec from "k6/execution";

const sseConnections = new Counter("sse_connections_total");
const sseSuccess = new Rate("sse_connection_success_rate");
const broadcastReceived = new Counter("broadcast_messages_received");

export const options = {
  scenarios: {
    sse_connections: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "2m", target: 10000 },
        { duration: "2m", target: 20000 },
        { duration: "6m", target: 20000 },
      ],
    },
    broadcast_sender: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      startTime: "5m",
    },
  },
  thresholds: {
    sse_connection_success_rate: ["rate>0.80"],
    broadcast_messages_received: ["count>=16000"], // 80% 이상 수신 목표
  },
};

const BASE_URL = __ENV.BASE_URL || "http://172.16.24.202:8081";
const TOKEN = __ENV.TOKEN;

export default function () {
  if (exec.scenario.name === "sse_connections") {
    sseConnection();
  } else if (exec.scenario.name === "broadcast_sender") {
    sendBroadcast();
  }
}

// ✅ 수정된 SSE 연결 (이벤트 핸들러 추가)
function sseConnection() {
  const start = Date.now();
  let connected = false;

  const options = {
    headers: {
      Accept: "text/event-stream",
      Authorization: `Bearer ${TOKEN}`,
    },
    timeout: "18s",
  };

  try {
    sse.open(
      `${BASE_URL}/api/v1/notifications/connect`,
      options,
      function (client) {
        // ✅ 연결 성공 핸들러
        client.on("open", function () {
          connected = true;
          sseConnections.add(1);
          sseSuccess.add(1);
          console.log(`VU ${__VU}: SSE Connected successfully`);
        });

        // ✅ 핵심 추가: 메시지 수신 핸들러
        client.on("event", function (event) {
          console.log(
            `VU ${__VU}: 브로드캐스트 메시지 수신! Data: ${event.data}`
          );

          // 브로드캐스트 메시지 카운팅
          broadcastReceived.add(1);

          // 추가 분석: 메시지 타입 확인
          try {
            const messageData = JSON.parse(event.data);
            console.log(
              `VU ${__VU}: 메시지 타입: ${messageData.type || "unknown"}`
            );
          } catch (e) {
            console.log(`VU ${__VU}: 텍스트 메시지 수신`);
          }
        });

        // ✅ 에러 핸들러
        client.on("error", function (error) {
          console.log(`VU ${__VU}: SSE 오류 - ${error.error()}`);
          sseSuccess.add(0);
        });

        // ✅ 연결 종료 핸들러
        client.on("close", function () {
          console.log(`VU ${__VU}: SSE 연결 종료`);
        });

        // 5분간 연결 유지
        sleep(300);
      }
    );
  } catch (error) {
    console.log(`VU ${__VU}: Connection failed - ${error.message}`);
    sseSuccess.add(0);
  }
}

function sendBroadcast() {
  console.log("=== 브로드캐스트 전송 시작 ===");
  sleep(10); // 추가 대기

  const response = http.get(`${BASE_URL}/test/notification-event/partner`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
  });

  console.log(`브로드캐스트 API 결과: ${response.status}`);

  if (response.status === 200) {
    console.log(
      "✅ 브로드캐스트 전송 성공 - SSE 클라이언트들의 메시지 수신 대기 중..."
    );
  } else {
    console.log(
      `❌ 브로드캐스트 전송 실패: ${response.status} - ${response.body}`
    );
  }

  // 메시지 전파 대기
  sleep(30);
}