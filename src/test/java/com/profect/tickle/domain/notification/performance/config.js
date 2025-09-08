// config.js - 환경별 설정 관리 (VU 동적 조정 기능 포함)
const configs = {
  development: {
    baseUrl: "http://localhost:8081",
    vus: 5, // VU 수
    sessionSec: 10, // 세션 시간
    maxRetries: 2,
    memberId: "50010",
    stages: [
      { duration: "10s", target: 5 },
      { duration: "15s", target: 5 },
      { duration: "10s", target: 0 },
    ],
    thresholds: {
      sse_open_ok: ["rate>0.90"],
      sse_stayed_full: ["rate>0.80"],
      sse_conn_alive_ms: ["p(95)<30000"],
      sse_connection_errors: ["count<10"],
      latency: ["p(95)<5000"],
    },
  },

  staging: {
    baseUrl: "http://172.16.24.202:8081",
    vus: 20, // VU 수
    sessionSec: 30, // 세션 시간
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
      latency: ["p(95)<3000"],
    },
  },

  production: {
    baseUrl: "http://172.16.24.202:8081",
    vus: 50, // 동시 사용자 50명
    sessionSec: 60, // 1분 세션 유지
    maxRetries: 3, // 최대 3회 재시도
    memberId: "50010", // 테스트 회원 ID
    stages: [
      { duration: "30s", target: 25 }, // 1단계: 30초간 25명까지 증가
      { duration: "60s", target: 50 }, // 2단계: 60초간 50명까지 증가
      { duration: "30s", target: 25 }, // 3단계: 30초간 25명까지 감소
      { duration: "20s", target: 0 }, // 4단계: 20초간 0명까지 감소
    ],
    thresholds: {
      sse_open_ok: ["rate>0.98"], // 98% 이상 연결 성공
      sse_stayed_full: ["rate>0.95"], // 95% 이상 세션 완료
      sse_conn_alive_ms: ["p(95)<65000"], // 60초 + 5초 여유
      sse_connection_errors: ["count<10"], // 에러 10개 이하
      latency: ["p(95)<2000"], // 첫 메시지 2초 이내
    },
  },
};

// 20% 단계별 증가 패턴 생성 함수
function generateStagesByPercentage(targetVus) {
  const stepRatio = 0.2; // 20%
  const stages = [];

  // 20% → 40% → 60% → 80% → 100% 단계별 증가
  for (let i = 1; i <= 5; i++) {
    const target = Math.min(Math.ceil(targetVus * stepRatio * i), targetVus);
    stages.push({ duration: "20s", target: target });
  }

  // 최대 부하 유지 단계
  stages.push({ duration: "60s", target: targetVus });

  // 종료 단계
  stages.push({ duration: "20s", target: 0 });

  return stages;
}

// VUS에 따른 임계값 조정 함수
function adjustThresholdsForVus(vus) {
  const scaleFactor = vus / 100; // 100 VU를 기준으로 스케일링

  return {
    sse_open_ok: [`rate>${Math.max(0.85, 0.95 - (scaleFactor - 1) * 0.02)}`],
    sse_stayed_full: [`rate>${Math.max(0.8, 0.9 - (scaleFactor - 1) * 0.03)}`],
    sse_conn_alive_ms: [
      `p(95)<${Math.min(120000, 65000 + (scaleFactor - 1) * 2000)}`,
    ],
    sse_connection_errors: [
      `count<${Math.min(100, Math.ceil(10 + scaleFactor * 2))}`,
    ],
    latency: [`p(95)<${Math.min(8000, 2000 + (scaleFactor - 1) * 300)}`],
  };
}

// 기존 동적 설정 조정 함수 (레거시 지원용)
const getAdjustedConfig = function (baseConfig, targetVus) {
  const scaleFactor = targetVus / baseConfig.vus;

  console.log(
      `📊 Scaling from ${
          baseConfig.vus
      } to ${targetVus} VUs (factor: ${scaleFactor.toFixed(2)})`
  );

  return {
    ...baseConfig,
    vus: targetVus,
    stages: baseConfig.stages.map((stage) => ({
      ...stage,
      target: stage.target === 0 ? 0 : Math.ceil(stage.target * scaleFactor),
    })),
    thresholds: {
      sse_open_ok: [`rate>${Math.max(0.9, 0.98 - (scaleFactor - 1) * 0.03)}`],
      sse_stayed_full: [
        `rate>${Math.max(0.85, 0.95 - (scaleFactor - 1) * 0.05)}`,
      ],
      sse_conn_alive_ms: [
        `p(95)<${Math.min(90000, 65000 + (scaleFactor - 1) * 5000)}`,
      ],
      sse_connection_errors: [
        `count<${Math.min(50, Math.ceil(10 * scaleFactor))}`,
      ],
      latency: [`p(95)<${Math.min(5000, 2000 + (scaleFactor - 1) * 500)}`],
    },
  };
};

export function getConfig() {
  const environment = __ENV.ENVIRONMENT || "development";
  const targetVus = Number(__ENV.VUS);

  let config = configs[environment];

  if (!config) {
    throw new Error(
        `Unknown environment: ${environment}. Available: ${Object.keys(
            configs
        ).join(", ")}`
    );
  }

  // VUS 환경변수로 동적 조정 + 20% 단계별 증가
  if (targetVus && targetVus !== config.vus) {
    config = {
      ...config,
      vus: targetVus,
      stages: generateStagesByPercentage(targetVus),
      thresholds: adjustThresholdsForVus(targetVus),
    };

    console.log(`🎯 Using 20% step ramp-up pattern for ${targetVus} VUs`);
    console.log(`📊 Stages: ${JSON.stringify(config.stages)}`);

    return config;
  }

  return {
    ...config,
    baseUrl: __ENV.BASE_URL || config.baseUrl,
    vus: config.vus,
    stages: config.stages,
    thresholds: config.thresholds,
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
