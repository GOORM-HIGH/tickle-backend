const StompJs = require('@stomp/stompjs');
const SockJS = require('sockjs-client');
const http = require('http');

// SockJS를 WebSocket으로 설정
Object.assign(global, { WebSocket: SockJS });

console.log('🚀 SockJS STOMP 테스트 시작');

// 로그인
function getToken() {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify({email: 'ahn3931@naver.com', password: '1234'});
    const req = http.request({
      hostname: 'localhost', port: 8081, path: '/api/v1/sign-in', method: 'POST',
      headers: {'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData)}
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve(JSON.parse(data).accessToken);
        } else {
          reject(new Error(`로그인 실패: ${res.statusCode}`));
        }
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

async function main() {
  try {
    console.log('🔑 토큰 획득 중...');
    const token = await getToken();
    console.log('✅ 토큰 획득 성공');

    console.log('🔌 SockJS STOMP 연결 시도...');
    const client = new StompJs.Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => console.log('STOMP:', str.substring(0, 100)),
      reconnectDelay: 0,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    let messageCount = 0;

    client.onConnect = (frame) => {
      console.log('✅ STOMP 연결 성공!');
      console.log('👤 연결 사용자:', frame.headers['user-name'] || '익명');
      
      // 채팅방 구독
      const subscription = client.subscribe('/topic/chat/1', (message) => {
        messageCount++;
        const data = JSON.parse(message.body);
        console.log(`📨 메시지 수신 #${messageCount}:`, data.content || data.type);
      });

      console.log('🚪 채팅방 참여...');
      client.publish({
        destination: '/app/chat.join',
        body: JSON.stringify({
          type: 'JOIN',
          chatRoomId: 1
        })
      });

      // 1초 후 테스트 메시지 전송
      setTimeout(() => {
        console.log('📤 테스트 메시지 전송...');
        client.publish({
          destination: '/app/chat.message',
          body: JSON.stringify({
            type: 'MESSAGE',
            chatRoomId: 1,
            content: `STOMP 테스트 메시지 ${Date.now()}`,
            messageType: 'TEXT'
          })
        });
      }, 1000);

      // 10초 후 종료
      setTimeout(() => {
        console.log('🚪 채팅방 나가기...');
        client.publish({
          destination: '/app/chat.leave',
          body: JSON.stringify({
            type: 'LEAVE',
            chatRoomId: 1
          })
        });

        setTimeout(() => {
          console.log(`🏁 테스트 완료! 총 ${messageCount}개 메시지 수신`);
          subscription.unsubscribe();
          client.deactivate();
          process.exit(0);
        }, 1000);
      }, 10000);
    };

    client.onStompError = (frame) => {
      console.error('❌ STOMP 에러:', frame.headers['message']);
      console.error('세부사항:', frame.body);
      process.exit(1);
    };

    client.onWebSocketError = (error) => {
      console.error('❌ WebSocket 에러:', error);
      process.exit(1);
    };

    client.onDisconnect = () => {
      console.log('🔌 연결 해제됨');
    };

    client.activate();

  } catch (error) {
    console.error('❌ 테스트 실패:', error.message);
    process.exit(1);
  }
}

main();
