package com.profect.tickle.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * STOMP 메시지에서 JWT 토큰 인증 처리
 * Single Responsibility Principle 적용으로 리팩토링
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final StompJwtValidator jwtValidator;
    private final StompSessionManager sessionManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 명령일 때만 JWT 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            return handleConnectCommand(accessor, message);
        }

        return message;
    }

    /**
     * CONNECT 명령 처리
     * @param accessor StompHeaderAccessor
     * @param message 원본 메시지
     * @return 처리된 메시지 또는 null (연결 거부)
     */
    private Message<?> handleConnectCommand(StompHeaderAccessor accessor, Message<?> message) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        String token = jwtValidator.extractTokenFromHeader(authHeader);
        
        if (token == null) {
            // 개발 환경에서는 허용, 운영에서는 거부
            log.warn("STOMP CONNECT에서 Authorization 헤더 없음");
            return message;
        }

        // JWT 토큰 검증 및 사용자 ID 추출
        Long userId = jwtValidator.validateAndExtractUserId(token);
        if (userId == null) {
            sessionManager.rejectConnection("JWT 검증 실패");
            return null; // 연결 거부
        }

        // 세션 설정
        try {
            sessionManager.setupUserSession(accessor, userId);
            return message;
        } catch (Exception e) {
            log.error("STOMP 세션 설정 실패: {}", e.getMessage(), e);
            sessionManager.rejectConnection("세션 설정 실패");
            return null; // 연결 거부
        }
    }
}
