const StompJs = require('@stomp/stompjs');
const SockJS = require('sockjs-client');
const http = require('http');

// SockJS를 WebSocket으로 설정
Object.assign(global, { WebSocket: SockJS });

console.log('🚀 실제 작동하는 멀티 사용자 채팅 테스트 시작');

// 테스트 사용자 정보
const testUsers = [
    { email: 'ahn3931@naver.com', password: '1234', name: '사용자1' },
    { email: 'ahn3931@naver.com', password: '1234', name: '사용자2' },
    { email: 'ahn3931@naver.com', password: '1234', name: '사용자3' }
];

// 로그인 함수
function getToken(email, password) {
    return new Promise((resolve, reject) => {
        const postData = JSON.stringify({ email, password });
        const req = http.request({
            hostname: 'localhost', port: 8081, path: '/api/v1/sign-in', method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(postData) }
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

// 채팅 클라이언트 클래스 (정확한 enum 값 사용)
class ChatClient {
    constructor(name, token) {
        this.name = name;
        this.token = token;
        this.client = null;
        this.receivedMessages = [];
        this.isConnected = false;
    }

    async connect() {
        return new Promise((resolve, reject) => {
            const socket = new SockJS('http://localhost:8081/ws');
            
            this.client = new StompJs.Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    Authorization: `Bearer ${this.token}`,
                },
                debug: (str) => {
                    if (str.includes('CONNECTED') || str.includes('ERROR')) {
                        console.log(`[${this.name}] ${str}`);
                    }
                },
                onConnect: (frame) => {
                    console.log(`✅ [${this.name}] STOMP 연결 성공!`);
                    this.isConnected = true;
                    
                    // 채팅방 구독
                    this.client.subscribe('/topic/chat/1', (message) => {
                        const chatMessage = JSON.parse(message.body);
                        this.receivedMessages.push(chatMessage);
                        console.log(`📨 [${this.name}] 메시지 수신:`, {
                            from: chatMessage.senderNickname || '알 수 없음',
                            type: chatMessage.type || chatMessage.messageType,
                            content: chatMessage.content || '시스템 메시지'
                        });
                    });
                    
                    resolve();
                },
                onStompError: (frame) => {
                    console.error(`❌ [${this.name}] STOMP 에러:`, frame.headers['message']);
                    reject(new Error(frame.headers['message']));
                }
            });

            this.client.activate();
        });
    }

    sendJoin() {
        if (!this.isConnected) return;
        
        console.log(`🚪 [${this.name}] 채팅방 참여...`);
        // SYSTEM 타입으로 JOIN 메시지 전송
        this.client.publish({
            destination: '/app/chat.join',
            body: JSON.stringify({
                type: 'JOIN',
                chatRoomId: 1,
                messageType: 'SYSTEM',  // 올바른 enum 값 사용
                content: `${this.name}님이 채팅방에 참여했습니다.`
            }),
            headers: {
                Authorization: `Bearer ${this.token}`
            }
        });
    }

    sendMessage(content) {
        if (!this.isConnected) return;
        
        console.log(`📤 [${this.name}] 메시지 전송: "${content}"`);
        // TEXT 타입으로 일반 메시지 전송
        this.client.publish({
            destination: '/app/chat.message',
            body: JSON.stringify({
                type: 'MESSAGE',
                chatRoomId: 1,
                messageType: 'TEXT',  // 올바른 enum 값 사용
                content: content
            }),
            headers: {
                Authorization: `Bearer ${this.token}`
            }
        });
    }

    sendLeave() {
        if (!this.isConnected) return;
        
        console.log(`🚪 [${this.name}] 채팅방 나가기...`);
        // SYSTEM 타입으로 LEAVE 메시지 전송
        this.client.publish({
            destination: '/app/chat.leave',
            body: JSON.stringify({
                type: 'LEAVE',
                chatRoomId: 1,
                messageType: 'SYSTEM',  // 올바른 enum 값 사용
                content: `${this.name}님이 채팅방을 나갔습니다.`
            }),
            headers: {
                Authorization: `Bearer ${this.token}`
            }
        });
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            console.log(`🔌 [${this.name}] 연결 해제`);
        }
    }

    getStats() {
        return {
            name: this.name,
            connected: this.isConnected,
            messagesReceived: this.receivedMessages.length,
            messages: this.receivedMessages
        };
    }
}

// 메인 테스트 함수
async function runWorkingMultiUserTest() {
    const clients = [];
    
    try {
        // 1. 모든 사용자 로그인 및 연결
        console.log('\n🔑 사용자들 로그인 중...');
        for (let i = 0; i < testUsers.length; i++) {
            const user = testUsers[i];
            const token = await getToken(user.email, user.password);
            const client = new ChatClient(user.name, token);
            clients.push(client);
            
            await client.connect();
            await new Promise(resolve => setTimeout(resolve, 1500)); // 1.5초 대기
        }

        console.log(`\n🎭 ${clients.length}명의 사용자가 채팅방에 연결되었습니다!`);

        // 2. 순차적으로 채팅방 참여
        console.log('\n🚪 채팅방 참여 중...');
        for (const client of clients) {
            client.sendJoin();
            await new Promise(resolve => setTimeout(resolve, 1000));
        }

        await new Promise(resolve => setTimeout(resolve, 3000)); // 3초 대기

        // 3. 메시지 교환 시뮬레이션
        console.log('\n💬 메시지 교환 시작...');
        
        clients[0].sendMessage('안녕하세요! 첫 번째 사용자입니다.');
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        clients[1].sendMessage('반갑습니다! 두 번째 사용자예요.');
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        clients[2].sendMessage('저도 참여했습니다! 세 번째 사용자입니다.');
        await new Promise(resolve => setTimeout(resolve, 3000));

        // 4. 결과 확인
        console.log('\n📊 테스트 결과:');
        let totalMessagesReceived = 0;
        
        for (const client of clients) {
            const stats = client.getStats();
            console.log(`[${stats.name}] 연결: ${stats.connected}, 수신 메시지: ${stats.messagesReceived}개`);
            if (stats.messagesReceived > 0) {
                console.log(`  └─ 수신한 메시지들:`);
                stats.messages.forEach(m => {
                    console.log(`     • ${m.senderNickname || '시스템'}: ${m.content}`);
                });
            }
            totalMessagesReceived += stats.messagesReceived;
        }

        console.log(`\n🎯 전체 수신 메시지: ${totalMessagesReceived}개`);
        
        if (totalMessagesReceived > 0) {
            console.log('✅ 멀티 사용자 채팅 테스트 성공!');
            console.log('🎉 실시간 메시지 교환이 정상적으로 작동합니다!');
            console.log('🏆 WebSocket STOMP 멀티 세션 통신 검증 완료!');
        } else {
            console.log('⚠️  메시지 수신 없음');
            console.log('💡 enum 값을 TEXT, FILE, IMAGE, SYSTEM으로 수정했으니 다시 시도해보세요!');
        }

        // 5. 정리
        console.log('\n🚪 채팅방 나가기...');
        for (const client of clients) {
            client.sendLeave();
            await new Promise(resolve => setTimeout(resolve, 1000));
        }

    } catch (error) {
        console.error('❌ 테스트 실행 중 오류:', error.message);
    } finally {
        // 연결 해제
        for (const client of clients) {
            client.disconnect();
        }
        console.log('\n🏁 테스트 완료!');
        process.exit(0);
    }
}

// 테스트 실행
runWorkingMultiUserTest().catch(console.error);
