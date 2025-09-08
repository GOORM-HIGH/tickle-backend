#!/usr/bin/env node

/**
 * 통합 성능 테스트 스위트
 * 사용법: node performance-test-suite.js [테스트타입]
 * 
 * 테스트 타입:
 * - http: HTTP API 성능 테스트 (k6)
 * - websocket: WebSocket 연결 테스트  
 * - multiuser: 멀티 사용자 채팅 테스트
 * - all: 모든 테스트 순차 실행
 */

const { execSync } = require('child_process');
const StompJs = require('@stomp/stompjs');
const SockJS = require('sockjs-client');
const http = require('http');

// SockJS를 WebSocket으로 설정
Object.assign(global, { WebSocket: SockJS });

class PerformanceTestSuite {
    constructor() {
        this.results = {
            http: null,
            websocket: null,
            multiuser: null
        };
    }

    // 메인 실행 함수
    async run(testType = 'all') {
        console.log('🚀 통합 성능 테스트 스위트 시작');
        console.log(`📋 실행할 테스트: ${testType}`);
        console.log('=' .repeat(50));

        try {
            switch (testType) {
                case 'http':
                    await this.runHttpTest();
                    break;
                case 'websocket':
                    await this.runWebSocketTest();
                    break;
                case 'multiuser':
                    await this.runMultiUserTest();
                    break;
                case 'all':
                    await this.runAllTests();
                    break;
                default:
                    console.error('❌ 지원하지 않는 테스트 타입:', testType);
                    this.showUsage();
                    return;
            }

            this.printSummary();
        } catch (error) {
            console.error('❌ 테스트 실행 중 오류:', error.message);
            process.exit(1);
        }
    }

    // HTTP API 성능 테스트
    async runHttpTest() {
        console.log('\n📊 HTTP API 성능 테스트 시작...');
        
        try {
            const result = execSync('k6 run --duration=30s --vus=10 chat-file-focused-test.js', {
                encoding: 'utf8',
                timeout: 60000
            });
            
            // k6 결과에서 TPS 추출
            const tpsMatch = result.match(/http_reqs\.+(\d+\.?\d*)/);
            const tps = tpsMatch ? parseFloat(tpsMatch[1]) : 0;
            
            this.results.http = {
                success: true,
                tps: tps,
                duration: '30s'
            };
            
            console.log(`✅ HTTP 테스트 완료: ${tps} TPS`);
        } catch (error) {
            console.error('❌ HTTP 테스트 실패:', error.message);
            this.results.http = { success: false, error: error.message };
        }
    }

    // WebSocket 연결 테스트
    async runWebSocketTest() {
        console.log('\n🔌 WebSocket 연결 테스트 시작...');
        
        try {
            const token = await this.getAuthToken();
            const connected = await this.testWebSocketConnection(token);
            
            this.results.websocket = {
                success: connected,
                protocol: 'STOMP over SockJS',
                auth: 'JWT'
            };
            
            console.log(connected ? '✅ WebSocket 연결 성공' : '❌ WebSocket 연결 실패');
        } catch (error) {
            console.error('❌ WebSocket 테스트 실패:', error.message);
            this.results.websocket = { success: false, error: error.message };
        }
    }

    // 멀티 사용자 채팅 테스트
    async runMultiUserTest() {
        console.log('\n👥 멀티 사용자 채팅 테스트 시작...');
        
        try {
            const result = await this.runMultiUserChat();
            
            this.results.multiuser = {
                success: result.totalMessages > 0,
                users: result.userCount,
                messages: result.totalMessages,
                realtime: true
            };
            
            console.log(`✅ 멀티 사용자 테스트 완료: ${result.userCount}명, ${result.totalMessages}개 메시지`);
        } catch (error) {
            console.error('❌ 멀티 사용자 테스트 실패:', error.message);
            this.results.multiuser = { success: false, error: error.message };
        }
    }

    // 모든 테스트 순차 실행
    async runAllTests() {
        await this.runHttpTest();
        await new Promise(resolve => setTimeout(resolve, 5000)); // 5초 대기
        
        await this.runWebSocketTest();
        await new Promise(resolve => setTimeout(resolve, 3000)); // 3초 대기
        
        await this.runMultiUserTest();
    }

    // JWT 토큰 가져오기
    getAuthToken() {
        return new Promise((resolve, reject) => {
            const postData = JSON.stringify({
                email: 'ahn3931@naver.com',
                password: '1234'
            });

            const req = http.request({
                hostname: 'localhost',
                port: 8081,
                path: '/api/v1/sign-in',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(postData)
                }
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

    // WebSocket 연결 테스트
    testWebSocketConnection(token) {
        return new Promise((resolve) => {
            const socket = new SockJS('http://localhost:8081/ws');
            
            const client = new StompJs.Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    Authorization: `Bearer ${token}`,
                },
                onConnect: () => {
                    client.deactivate();
                    resolve(true);
                },
                onStompError: () => {
                    resolve(false);
                }
            });

            client.activate();
            
            // 5초 타임아웃
            setTimeout(() => {
                client.deactivate();
                resolve(false);
            }, 5000);
        });
    }

    // 멀티 사용자 채팅 테스트 (간소화 버전)
    async runMultiUserChat() {
        const userCount = 3;
        const clients = [];
        let totalMessages = 0;

        try {
            // 사용자들 연결
            for (let i = 0; i < userCount; i++) {
                const token = await this.getAuthToken();
                const client = await this.createChatClient(`사용자${i + 1}`, token);
                clients.push(client);
            }

            // 메시지 교환
            await new Promise(resolve => setTimeout(resolve, 2000));
            
            for (let i = 0; i < clients.length; i++) {
                clients[i].sendMessage(`안녕하세요! ${i + 1}번째 사용자입니다.`);
                await new Promise(resolve => setTimeout(resolve, 1000));
            }

            await new Promise(resolve => setTimeout(resolve, 3000));

            // 결과 수집
            clients.forEach(client => {
                totalMessages += client.getMessageCount();
            });

            // 정리
            clients.forEach(client => client.disconnect());

            return { userCount, totalMessages };
        } catch (error) {
            clients.forEach(client => client.disconnect());
            throw error;
        }
    }

    // 채팅 클라이언트 생성
    createChatClient(name, token) {
        return new Promise((resolve, reject) => {
            const socket = new SockJS('http://localhost:8081/ws');
            let messageCount = 0;
            
            const client = new StompJs.Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    Authorization: `Bearer ${token}`,
                },
                onConnect: () => {
                    client.subscribe('/topic/chat/1', () => {
                        messageCount++;
                    });
                    
                    resolve({
                        name,
                        sendMessage: (content) => {
                            client.publish({
                                destination: '/app/chat.message',
                                body: JSON.stringify({
                                    type: 'MESSAGE',
                                    chatRoomId: 1,
                                    messageType: 'TEXT',
                                    content: content
                                })
                            });
                        },
                        getMessageCount: () => messageCount,
                        disconnect: () => client.deactivate()
                    });
                },
                onStompError: reject
            });

            client.activate();
        });
    }

    // 결과 요약 출력
    printSummary() {
        console.log('\n' + '='.repeat(50));
        console.log('📋 테스트 결과 요약');
        console.log('='.repeat(50));

        if (this.results.http) {
            console.log(`HTTP API: ${this.results.http.success ? '✅' : '❌'} ${this.results.http.tps || 0} TPS`);
        }

        if (this.results.websocket) {
            console.log(`WebSocket: ${this.results.websocket.success ? '✅' : '❌'} ${this.results.websocket.protocol || 'N/A'}`);
        }

        if (this.results.multiuser) {
            console.log(`멀티유저: ${this.results.multiuser.success ? '✅' : '❌'} ${this.results.multiuser.users || 0}명/${this.results.multiuser.messages || 0}개 메시지`);
        }
    }

    // 사용법 출력
    showUsage() {
        console.log('\n사용법:');
        console.log('  node performance-test-suite.js [테스트타입]');
        console.log('\n테스트 타입:');
        console.log('  http      - HTTP API 성능 테스트');
        console.log('  websocket - WebSocket 연결 테스트');
        console.log('  multiuser - 멀티 사용자 채팅 테스트');
        console.log('  all       - 모든 테스트 순차 실행 (기본값)');
    }
}

// 메인 실행부
if (require.main === module) {
    const testType = process.argv[2] || 'all';
    const suite = new PerformanceTestSuite();
    suite.run(testType);
}

module.exports = PerformanceTestSuite;
