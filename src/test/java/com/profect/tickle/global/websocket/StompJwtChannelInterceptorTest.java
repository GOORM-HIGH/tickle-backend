package com.profect.tickle.global.websocket;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StompJwtChannelInterceptor 단위 테스트
 * 
 * 테스트 범위:
 * - STOMP CONNECT 시 JWT 토큰 인증 처리
 * - 유효한 JWT 토큰으로 연결 성공
 * - 유효하지 않은 JWT 토큰으로 연결 실패
 * - JWT 토큰이 없는 경우 처리
 * - CONNECT가 아닌 다른 명령 처리
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("STOMP JWT Channel Interceptor 단위 테스트")
class StompJwtChannelInterceptorTest {

    @InjectMocks
    private StompJwtChannelInterceptor stompJwtChannelInterceptor;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MessageChannel messageChannel;

    private Message<?> connectMessage;
    private Message<?> subscribeMessage;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(6L)
                .email("ahn3931@naver.com")
                .nickname("테스트사용자")
                .build();

        // STOMP CONNECT 메시지 생성
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.addNativeHeader("Authorization", "Bearer valid.jwt.token");
        connectMessage = MessageBuilder.createMessage("connect payload", connectAccessor.getMessageHeaders());

        // STOMP SUBSCRIBE 메시지 생성
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeMessage = MessageBuilder.createMessage("subscribe payload", subscribeAccessor.getMessageHeaders());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-001: 유효한 JWT 토큰으로 STOMP 연결 성공")
    void shouldConnectSuccessfullyWithValidJwtToken() {
        // Given
        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getUserId("valid.jwt.token")).thenReturn(6L);

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(connectMessage, messageChannel);

        // Then
        assertNotNull(result);
        // JWT 인증이 성공했으므로 메시지가 반환되어야 함
        verify(jwtUtil).validateToken("valid.jwt.token");
        verify(jwtUtil).getUserId("valid.jwt.token");
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-002: JWT에서 userId가 없을 때 이메일로 사용자 조회")
    void shouldFindUserByEmailWhenUserIdIsNull() {
        // Given
        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getUserId("valid.jwt.token")).thenReturn(null);
        when(jwtUtil.getEmail("valid.jwt.token")).thenReturn("ahn3931@naver.com");
        when(memberRepository.findByEmail("ahn3931@naver.com")).thenReturn(Optional.of(testMember));

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(connectMessage, messageChannel);

        // Then
        assertNotNull(result);
        // JWT 인증이 성공했으므로 메시지가 반환되어야 함
        verify(jwtUtil).validateToken("valid.jwt.token");
        verify(jwtUtil).getUserId("valid.jwt.token");
        verify(jwtUtil).getEmail("valid.jwt.token");
        verify(memberRepository).findByEmail("ahn3931@naver.com");
    }

    @Test
    @DisplayName("TC-STOMP-JWT-003: 유효하지 않은 JWT 토큰으로 연결 실패")
    void shouldRejectConnectionWithInvalidJwtToken() {
        // Given
        StompHeaderAccessor invalidAuthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        invalidAuthAccessor.addNativeHeader("Authorization", "Bearer invalid.jwt.token");
        Message<?> invalidAuthMessage = MessageBuilder.createMessage("connect payload", invalidAuthAccessor.getMessageHeaders());
        
        when(jwtUtil.validateToken("invalid.jwt.token")).thenReturn(false);

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(invalidAuthMessage, messageChannel);

        // Then
        assertNull(result); // 연결 거부
        
        verify(jwtUtil).validateToken("invalid.jwt.token");
        verify(jwtUtil, never()).getUserId(anyString());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-004: JWT 토큰이 없는 경우 연결 허용 (개발 환경)")
    void shouldAllowConnectionWithoutJwtToken() {
        // Given
        StompHeaderAccessor noAuthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> noAuthMessage = MessageBuilder.createMessage("connect payload", noAuthAccessor.getMessageHeaders());

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(noAuthMessage, messageChannel);

        // Then
        assertNotNull(result); // 개발 환경에서는 허용
        
        verify(jwtUtil, never()).validateToken(anyString());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-005: CONNECT가 아닌 다른 STOMP 명령은 그대로 통과")
    void shouldPassThroughNonConnectCommands() {
        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(subscribeMessage, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(subscribeMessage, result);
        
        verify(jwtUtil, never()).validateToken(anyString());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-006: JWT 토큰 검증 중 예외 발생 시 연결 거부")
    void shouldRejectConnectionWhenJwtValidationThrowsException() {
        // Given
        StompHeaderAccessor exceptionAuthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        exceptionAuthAccessor.addNativeHeader("Authorization", "Bearer exception.jwt.token");
        Message<?> exceptionAuthMessage = MessageBuilder.createMessage("connect payload", exceptionAuthAccessor.getMessageHeaders());
        
        when(jwtUtil.validateToken("exception.jwt.token")).thenThrow(new RuntimeException("JWT 검증 오류"));

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(exceptionAuthMessage, messageChannel);

        // Then
        assertNull(result); // 연결 거부
        
        verify(jwtUtil).validateToken("exception.jwt.token");
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("TC-STOMP-JWT-007: 사용자 정보를 찾을 수 없을 때 연결 거부")
    void shouldRejectConnectionWhenUserNotFound() {
        // Given
        StompHeaderAccessor notFoundAuthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        notFoundAuthAccessor.addNativeHeader("Authorization", "Bearer notfound.jwt.token");
        Message<?> notFoundAuthMessage = MessageBuilder.createMessage("connect payload", notFoundAuthAccessor.getMessageHeaders());
        
        when(jwtUtil.validateToken("notfound.jwt.token")).thenReturn(true);
        when(jwtUtil.getUserId("notfound.jwt.token")).thenReturn(null);
        when(jwtUtil.getEmail("notfound.jwt.token")).thenReturn("nonexistent@example.com");
        when(memberRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(notFoundAuthMessage, messageChannel);

        // Then
        assertNull(result); // 연결 거부
        
        verify(jwtUtil).validateToken("notfound.jwt.token");
        verify(jwtUtil).getUserId("notfound.jwt.token");
        verify(jwtUtil).getEmail("notfound.jwt.token");
        verify(memberRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("TC-STOMP-JWT-008: Bearer가 아닌 다른 인증 방식은 무시")
    void shouldIgnoreNonBearerAuthentication() {
        // Given
        StompHeaderAccessor customAuthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        customAuthAccessor.addNativeHeader("Authorization", "Basic dXNlcjpwYXNz");
        Message<?> customAuthMessage = MessageBuilder.createMessage("connect payload", customAuthAccessor.getMessageHeaders());

        // When
        Message<?> result = stompJwtChannelInterceptor.preSend(customAuthMessage, messageChannel);

        // Then
        assertNotNull(result); // 개발 환경에서는 허용
        
        verify(jwtUtil, never()).validateToken(anyString());
        verify(memberRepository, never()).findByEmail(anyString());
    }
}
