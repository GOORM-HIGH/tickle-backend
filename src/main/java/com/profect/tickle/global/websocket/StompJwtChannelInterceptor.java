package com.profect.tickle.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP 메시지에서 JWT 토큰 인증 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    // private final JwtUtil jwtUtil; // JWT 유틸리티 주입 필요

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 모든 STOMP 명령에서 Authorization 헤더 처리
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("STOMP {} 명령에서 JWT 토큰 발견: {}", accessor.getCommand(), token.substring(0, Math.min(50, token.length())) + "...");
            
            // JWT 토큰을 메시지 헤더에 보존 (메시지 핸들러에서 접근 가능하도록)
            accessor.setHeader("JWT_TOKEN", token);
            
            // CONNECT 명령일 때만 인증 처리
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                try {
                    // JWT 토큰 검증 (실제 구현 필요)
                    // if (jwtUtil.validateToken(token)) {
                    //     String userId = jwtUtil.extractUserId(token);
                    //     accessor.setUser(() -> userId);
                    //     log.info("STOMP JWT 인증 성공: {}", userId);
                    // } else {
                    //     log.error("STOMP JWT 토큰 검증 실패");
                    //     return null; // 연결 거부
                    // }

                    // 임시로 인증 통과 처리
                    log.info("STOMP 연결 허용 (임시)");

                } catch (Exception e) {
                    log.error("STOMP JWT 처리 오류: {}", e.getMessage());
                    return null; // 연결 거부
                }
            }
        } else {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.warn("STOMP CONNECT에서 Authorization 헤더 없음");
                // 개발 환경에서는 허용, 운영에서는 거부
                // return null;
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                log.warn("STOMP SEND에서 Authorization 헤더 없음 - 메시지: {}", accessor.getDestination());
            }
        }

        return message;
    }
}
