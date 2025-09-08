import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const chatApiSuccessRate = new Rate('chat_api_success');

export let options = {
  stages: [
    { duration: '1m', target: 10 }, // 1분간 10명으로 증가
    { duration: '3m', target:30 }, // 3분간 30명으로 증가
    { duration: '2m', target: 50 }, // 2분간 50명으로 증가
    { duration: '5m', target: 50 }, // 5분간 50명 유지
    { duration: '2m', target: 0 }, // 2분간 0명으로 감소
  ],
  thresholds: {
    'errors': ['rate<0.05'], // 에러율 5% 미만
    'chat_api_success': ['rate>0.95'], // 채팅 API 성공률 95% 이상
    'http_req_duration': ['p(95)<1000'], // 95% 요청 시간 1초 미만
    'http_req_failed': ['rate<0.05'], // HTTP 실패율 5% 미만
  },
};

const BASE_URL = 'http://localhost:8081';

export default function() {
  // 1. 로그인하여 JWT 토큰 획득
  const loginResponse = http.post(`${BASE_URL}/api/v1/sign-in`,
    JSON.stringify({
      email: 'ahn3931@naver.com',
      password: '1234'
    }),
    {
      headers: { 'Content-Type': 'application/json' }
    }
  );
  
  const loginSuccess = check(loginResponse, {
    'login successful': (r) => r.status === 200,
    'login response has token': (r) => r.json('accessToken') !== undefined,
  });
  
  if (!loginSuccess) {
    errorRate.add(1);
    return;
  }
  
  const token = loginResponse.json('accessToken');
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
  
  // ===== 채팅 기능 테스트 =====
  
  // 2. 내 채팅방 목록 조회
  const myRoomsResponse = http.get(`${BASE_URL}/api/v1/chat/participants/my-rooms`, {
    headers: headers
  });
  
  const myRoomsSuccess = check(myRoomsResponse, {
    'my chat rooms retrieved': (r) => r.status === 200,
    'my rooms response has data': (r) => r.json('data') !== undefined,
  });
  
  if (myRoomsSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // 3. 채팅방 메시지 조회
  const messagesResponse = http.get(`${BASE_URL}/api/v1/chat/rooms/1/messages?page=0&size=20`, {
    headers: headers
  });
  
  const messagesSuccess = check(messagesResponse, {
    'chat messages retrieved': (r) => r.status === 200,
    'messages response has data': (r) => r.json('data') !== undefined,
  });
  
  if (messagesSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // 4. 채팅방 온라인 사용자 수 조회
  const onlineUsersResponse = http.get(`${BASE_URL}/api/v1/chat/rooms/1/online`, {
    headers: headers
  });
  
  const onlineUsersSuccess = check(onlineUsersResponse, {
    'online users retrieved': (r) => r.status === 200,
  });
  
  if (onlineUsersSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // 5. 먼저 채팅방에 참여
  const joinResponse = http.post(`${BASE_URL}/api/v1/chat/participants/rooms/1/join`, 
    JSON.stringify({}), {
    headers: headers
  });
  
  const joinSuccess = check(joinResponse, {
    'chat room join successful': (r) => r.status === 201 || r.status === 200, // 201: 신규 참여, 200: 이미 참여 중
  });
  
  if (joinSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // 6. 읽지 않은 메시지 개수 조회 (참여 후)
  const unreadCountResponse = http.get(`${BASE_URL}/api/v1/chat/participants/rooms/1/unread-count`, {
    headers: headers
  });
  
  const unreadCountSuccess = check(unreadCountResponse, {
    'unread count retrieved': (r) => r.status === 200,
  });
  
  if (unreadCountSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // 7. 채팅방 참여 여부 확인
  const participationResponse = http.get(`${BASE_URL}/api/v1/chat/rooms/1/participation`, {
    headers: headers
  });
  
  const participationSuccess = check(participationResponse, {
    'participation status retrieved': (r) => r.status === 200,
  });
  
  if (participationSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  // ===== 파일 기능 테스트 =====
  
  // 8. 파일 업로드 (작은 텍스트 파일)
  const fileContent = 'This is a test file for performance testing';
  const fileData = {
    file: http.file(fileContent, 'test-file.txt', 'text/plain'),
    type: 'CHAT'
  };
  
  const uploadResponse = http.post(`${BASE_URL}/api/v1/files/upload`, fileData, {
    headers: {
      'Authorization': `Bearer ${token}`,
    }
  });
  
  const uploadSuccess = check(uploadResponse, {
    'file upload successful': (r) => r.status === 200,
    'file upload response has data': (r) => r.json('data') !== undefined,
  });
  
  if (uploadSuccess) {
    chatApiSuccessRate.add(1);
  } else {
    errorRate.add(1);
  }
  
  sleep(1);
}
