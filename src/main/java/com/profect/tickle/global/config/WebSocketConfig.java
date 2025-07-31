package com.profect.tickle.global.config;

import com.profect.tickle.domain.chat.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 전역 WebSocket 설정 클래스
 * 모든 도메인에서 WebSocket 기능을 사용할 수 있도록 중앙 집중식 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 채팅 관련 WebSocket 엔드포인트
        registry.addHandler(chatWebSocketHandler, "/ws/chat/{chatRoomId}")
                .setAllowedOriginPatterns("*")  // 개발 환경용 (실제 운영에서는 특정 도메인으로 제한)
                .withSockJS()  // SockJS 폴백 지원 (프론트엔드 호환성)
                .setSessionCookieNeeded(false);  // CORS 환경에서 쿠키 불필요

        // 향후 다른 도메인의 WebSocket 핸들러도 여기에 추가 가능
        // 예: 알림 시스템, 실시간 업데이트 등
        // registry.addHandler(notificationWebSocketHandler, "/ws/notification/{userId}")
        //         .setAllowedOriginPatterns("*")
        //         .withSockJS();
    }
}
