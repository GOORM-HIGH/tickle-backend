package com.profect.tickle.global.websocket;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * STOMP WebSocket 통합 테스트
 * 
 * 테스트 범위:
 * - STOMP 연결 성공
 * - JWT 인증 테스트
 * - 기본 메시지 구독
 * - 연결 해제
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("STOMP WebSocket 통합 테스트")
class StompWebSocketIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private MemberRepository memberRepository;

    private WebSocketStompClient stompClient;
    private Member testMember;
    private String validJwtToken;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(6L)
                .email("ahn3931@naver.com")
                .nickname("테스트사용자")
                .build();

        validJwtToken = "valid.jwt.token.here";

        // WebSocket STOMP 클라이언트 설정
        StandardWebSocketClient standardClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(standardClient)));
        stompClient = new WebSocketStompClient(sockJsClient);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-001: 기본 STOMP 연결 성공")
    void shouldConnectSuccessfully() throws Exception {
        // When
        StompSession session = connectToWebSocket();
        
        // Then
        assertNotNull(session);
        assertTrue(session.isConnected());
        
        // 연결 해제
        session.disconnect();
        TimeUnit.MILLISECONDS.sleep(100);
        assertFalse(session.isConnected());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-002: 기본 메시지 구독 성공")
    void shouldSubscribeSuccessfully() throws Exception {
        // When
        StompSession session = connectToWebSocket();
        assertTrue(session.isConnected());
        
        // 구독 테스트
        session.subscribe("/topic/test", new StompSessionHandler() {
            @Override
            public void afterConnected(StompSession session, org.springframework.messaging.simp.stomp.StompHeaders connectedHeaders) {}
            
            @Override
            public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command, 
                                     org.springframework.messaging.simp.stomp.StompHeaders headers, byte[] payload, Throwable exception) {}
            
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {}
            
            @Override
            public Class<?> getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders headers, Object payload) {}
        });
        
        // Then
        assertTrue(session.isConnected());
        
        // 연결 해제
        session.disconnect();
        TimeUnit.MILLISECONDS.sleep(100);
        assertFalse(session.isConnected());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-004: WebSocket 서버 상태 확인")
    void shouldVerifyWebSocketServerStatus() throws Exception {
        // When
        StompSession session = connectToWebSocket();
        
        // Then
        assertNotNull(session);
        assertTrue(session.isConnected());
        
        // 연결 해제
        session.disconnect();
        TimeUnit.MILLISECONDS.sleep(100);
        assertFalse(session.isConnected());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-005: 연결 해제 시 정리 작업")
    void shouldCleanupOnDisconnect() throws Exception {
        // When
        StompSession session = connectToWebSocket();
        assertTrue(session.isConnected());
        
        // 연결 해제
        session.disconnect();
        TimeUnit.MILLISECONDS.sleep(100);
        
        // Then
        assertFalse(session.isConnected());
    }

    // 헬퍼 메서드들
    private StompSession connectToWebSocket() throws Exception {
        StompSessionHandler sessionHandler = new StompSessionHandler() {
            @Override
            public void afterConnected(StompSession session, org.springframework.messaging.simp.stomp.StompHeaders connectedHeaders) {}

            @Override
            public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command, 
                                     org.springframework.messaging.simp.stomp.StompHeaders headers, byte[] payload, Throwable exception) {
                // 예외 발생 시 로그만 출력
                System.out.println("WebSocket 연결 예외: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                // 전송 오류 시 로그만 출력
                System.out.println("WebSocket 전송 오류: " + exception.getMessage());
            }

            @Override
            public Class<?> getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders headers, Object payload) {}
        };

        return stompClient.connect("ws://localhost:8081/ws", sessionHandler).get(5, TimeUnit.SECONDS);
    }
}
