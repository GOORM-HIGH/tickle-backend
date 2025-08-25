package com.profect.tickle.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * STOMP WebSocket + JWT 통합 테스트
 * 
 * 테스트 범위:
 * - STOMP 연결 시 JWT 인증
 * - 인증된 사용자의 메시지 구독
 * - 인증된 사용자의 메시지 발행
 * - 인증되지 않은 사용자의 연결 거부
 * - JWT 토큰 만료 시 처리
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("STOMP WebSocket + JWT 통합 테스트")
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
    private String invalidJwtToken;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(6L)
                .email("ahn3931@naver.com")
                .nickname("테스트사용자")
                .build();

        validJwtToken = "valid.jwt.token.here";
        invalidJwtToken = "invalid.jwt.token.here";

        // WebSocket STOMP 클라이언트 설정
        StandardWebSocketClient standardClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(standardClient)));
        stompClient = new WebSocketStompClient(sockJsClient);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-001: 유효한 JWT로 STOMP 연결 성공")
    void shouldConnectSuccessfullyWithValidJwt() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenReturn(true);
        when(jwtUtil.getUserId(validJwtToken)).thenReturn(6L);
        when(memberRepository.findById(6L)).thenReturn(java.util.Optional.of(testMember));

        // When & Then
        StompSession session = connectWithJwt(validJwtToken);
        assertNotNull(session);
        assertTrue(session.isConnected());

        verify(jwtUtil).validateToken(validJwtToken);
        verify(jwtUtil).getUserId(validJwtToken);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-002: 유효하지 않은 JWT로 STOMP 연결 실패")
    void shouldRejectConnectionWithInvalidJwt() throws Exception {
        // Given
        when(jwtUtil.validateToken(invalidJwtToken)).thenReturn(false);

        // When & Then
        assertThrows(Exception.class, () -> connectWithJwt(invalidJwtToken));

        verify(jwtUtil).validateToken(invalidJwtToken);
        verify(jwtUtil, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-003: JWT 토큰이 없는 경우 연결 거부")
    void shouldRejectConnectionWithoutJwt() throws Exception {
        // When & Then
        assertThrows(Exception.class, () -> connectWithoutJwt());

        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-004: JWT 토큰 만료 시 연결 거부")
    void shouldRejectConnectionWithExpiredJwt() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenThrow(new RuntimeException("Token expired"));

        // When & Then
        assertThrows(Exception.class, () -> connectWithJwt(validJwtToken));

        verify(jwtUtil).validateToken(validJwtToken);
        verify(jwtUtil, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-005: 인증된 사용자의 메시지 구독 성공")
    void shouldSubscribeSuccessfullyWithAuthenticatedUser() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenReturn(true);
        when(jwtUtil.getUserId(validJwtToken)).thenReturn(6L);
        when(memberRepository.findById(6L)).thenReturn(java.util.Optional.of(testMember));

        // When
        StompSession session = connectWithJwt(validJwtToken);
        CompletableFuture<String> messageFuture = new CompletableFuture<>();

        session.subscribe("/topic/chat/1", new StompFrameHandler() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageFuture.complete(payload.toString());
            }

            @Override
            public Class<?> getPayloadType(StompHeaders headers) {
                return String.class;
            }
        });

        // Then
        assertTrue(session.isConnected());
        verify(jwtUtil).validateToken(validJwtToken);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-006: 인증된 사용자의 메시지 발행 성공")
    void shouldPublishMessageSuccessfullyWithAuthenticatedUser() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenReturn(true);
        when(jwtUtil.getUserId(validJwtToken)).thenReturn(6L);
        when(memberRepository.findById(6L)).thenReturn(java.util.Optional.of(testMember));

        String testMessage = "Hello, WebSocket!";

        // When
        StompSession session = connectWithJwt(validJwtToken);
        session.send("/app/chat/1", testMessage);

        // Then
        assertTrue(session.isConnected());
        verify(jwtUtil).validateToken(validJwtToken);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-007: 사용자별 메시지 전송 성공")
    void shouldSendUserSpecificMessageSuccessfully() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenReturn(true);
        when(jwtUtil.getUserId(validJwtToken)).thenReturn(6L);
        when(memberRepository.findById(6L)).thenReturn(java.util.Optional.of(testMember));

        // When
        StompSession session = connectWithJwt(validJwtToken);
        session.send("/app/chat/private", "Private message");

        // Then
        assertTrue(session.isConnected());
        verify(jwtUtil).validateToken(validJwtToken);
    }

    @Test
    @DisplayName("TC-STOMP-INTEGRATION-008: 연결 해제 시 정리 작업 수행")
    void shouldCleanupResourcesOnDisconnect() throws Exception {
        // Given
        when(jwtUtil.validateToken(validJwtToken)).thenReturn(true);
        when(jwtUtil.getUserId(validJwtToken)).thenReturn(6L);
        when(memberRepository.findById(6L)).thenReturn(java.util.Optional.of(testMember));

        // When
        StompSession session = connectWithJwt(validJwtToken);
        assertTrue(session.isConnected());

        session.disconnect();
        TimeUnit.MILLISECONDS.sleep(100); // 정리 작업 대기

        // Then
        assertFalse(session.isConnected());
        verify(jwtUtil).validateToken(validJwtToken);
    }

    // 헬퍼 메서드들
    private StompSession connectWithJwt(String jwtToken) throws Exception {
        StompSessionHandler sessionHandler = new StompSessionHandler() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {}

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                throw new RuntimeException(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                throw new RuntimeException(exception);
            }

            @Override
            public Class<?> getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {}
        };

        return stompClient.connect("ws://localhost:8080/ws", sessionHandler, 
                createHeadersWithJwt(jwtToken)).get(5, TimeUnit.SECONDS);
    }

    private StompSession connectWithoutJwt() throws Exception {
        StompSessionHandler sessionHandler = new StompSessionHandler() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {}

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                throw new RuntimeException(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                throw new RuntimeException(exception);
            }

            @Override
            public Class<?> getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {}
        };

        return stompClient.connect("ws://localhost:8080/ws", sessionHandler).get(5, TimeUnit.SECONDS);
    }

    private org.springframework.messaging.simp.stomp.StompHeaders createHeadersWithJwt(String jwtToken) {
        org.springframework.messaging.simp.stomp.StompHeaders headers = new org.springframework.messaging.simp.stomp.StompHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);
        return headers;
    }
}
