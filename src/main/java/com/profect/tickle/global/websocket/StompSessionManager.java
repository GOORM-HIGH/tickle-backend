package com.profect.tickle.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

/**
 * STOMP 세션 관리 전용 서비스
 * Single Responsibility Principle 적용
 */
@Service
@Slf4j
public class StompSessionManager {

    /**
     * STOMP 세션에 사용자 정보 설정
     * @param accessor StompHeaderAccessor
     * @param userId 사용자 ID
     */
    public void setupUserSession(StompHeaderAccessor accessor, Long userId) {
        try {
            // 세션에 사용자 정보 저장
            final Long finalUserId = userId;
            accessor.setUser(() -> finalUserId.toString());
            accessor.setHeader("userId", finalUserId);
            
            // SessionAttributes에도 저장 (영구 보관용)
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("userId", finalUserId);
                accessor.getSessionAttributes().put("userIdString", finalUserId.toString());
            }
            
            log.info("STOMP 세션 설정 완료: userId={}", finalUserId);
            
        } catch (Exception e) {
            log.error("STOMP 세션 설정 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("세션 설정 실패", e);
        }
    }

    /**
     * STOMP 연결 거부 처리
     * @param reason 거부 사유
     */
    public void rejectConnection(String reason) {
        log.warn("STOMP 연결 거부: {}", reason);
        // 연결 거부 로직 (필요시 추가)
    }
}
