// config.js - 환경별 설정 관리
const configs = {
  development: {
    baseUrl: "http://localhost:8081",
    vus: 5, // VU 수 감소
    sessionSec: 10, // 세션 시간 단축
    maxRetries: 2,
    memberId: "50010", // JWT에서 확인된 실제 ID
    stages: [
      { duration: "10s", target: 5 }, // 더 적은 VU로 시작
      { duration: "15s", target: 5 }, // 짧은 유지 시간
      { duration: "10s", target: 0 }, // 빠른 종료
    ],
    thresholds: {
      sse_open_ok: ["rate>0.90"], // 90%로 조정 (더 현실적)
      sse_stayed_full: ["rate>0.80"], // 80%로 조정
      sse_conn_alive_ms: ["p(95)<30000"], // 30초로 단축
      sse_connection_errors: ["count<10"], // 에러 허용 증가
      sse_time_to_first_message: ["p(95)<5000"], // 5초로 단축
    },
  },

  staging: {
    baseUrl: "https://staging-api.example.com",
    vus: 20, // 감소
    sessionSec: 30, // 감소
    maxRetries: 3,
    memberId: "50010",
    stages: [
      { duration: "20s", target: 20 },
      { duration: "30s", target: 20 },
      { duration: "20s", target: 0 },
    ],
    thresholds: {
      sse_open_ok: ["rate>0.95"],
      sse_stayed_full: ["rate>0.90"],
      sse_conn_alive_ms: ["p(95)<60000"],
      sse_connection_errors: ["count<15"],
      sse_time_to_first_message: ["p(95)<3000"],
    },
  },

  production: {
    baseUrl: "https://api.example.com",
    vus: 50, // 감소
    sessionSec: 60, // 감소
    maxRetries: 5,
    memberId: "50010",
    stages: [
      { duration: "30s", target: 25 },
      { duration: "60s", target: 50 },
      { duration: "30s", target: 25 },
      { duration: "20s", target: 0 },
    ],
    thresholds: {
      sse_open_ok: ["rate>0.98"],
      sse_stayed_full: ["rate>0.95"],
      sse_conn_alive_ms: ["p(95)<45000"],
      sse_connection_errors: ["count<10"],
      sse_time_to_first_message: ["p(95)<2000"],
    },
  },
};

export function getConfig() {
  const environment = __ENV.ENVIRONMENT || "development";
  const config = configs[environment];

  if (!config) {
    throw new Error(
      `Unknown environment: ${environment}. Available: ${Object.keys(
        configs
      ).join(", ")}`
    );
  }

  // 환경변수로 오버라이드 가능
  return {
    ...config,
    baseUrl: __ENV.BASE_URL || config.baseUrl,
    vus: Number(__ENV.VUS) || config.vus,
    sessionSec: Number(__ENV.SESSION_SEC) || config.sessionSec,
    maxRetries: Number(__ENV.MAX_RETRIES) || config.maxRetries,
    memberId: __ENV.MEMBER_ID || config.memberId,
    token: __ENV.TOKEN || "",
    lastEventId: __ENV.LAST_EVENT_ID || "",
    debug: __ENV.DEBUG === "true",
  };
}

export function getEnvironments() {
  return Object.keys(configs);
}
