package com.profect.tickle.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;

/**
 * 전역 WebSocket 세션 관리자
 * 모든 도메인에서 WebSocket 세션을 효율적으로 관리
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    // 전역 세션 관리 맵들
    private final ConcurrentMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionToUserId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionToRoomId = new ConcurrentHashMap<>();

    /**
     * 세션 등록
     */
    public void registerSession(String sessionId, WebSocketSession session, String userId, String roomId) {
        log.info("WebSocket 세션 등록: sessionId={}, userId={}, roomId={}", sessionId, userId, roomId);

        activeSessions.put(sessionId, session);
        sessionToUserId.put(sessionId, userId);
        sessionToRoomId.put(sessionId, roomId);

        log.info("현재 활성 세션 수: {}", activeSessions.size());
    }

    /**
     * 세션 제거
     */
    public void removeSession(String sessionId) {
        String userId = sessionToUserId.remove(sessionId);
        String roomId = sessionToRoomId.remove(sessionId);
        WebSocketSession session = activeSessions.remove(sessionId);

        if (session != null) {
            log.info("WebSocket 세션 제거: sessionId={}, userId={}, roomId={}", sessionId, userId, roomId);
        }

        log.info("현재 활성 세션 수: {}", activeSessions.size());
    }

    /**
     * 특정 사용자의 세션 조회
     */
    public Set<String> getSessionsByUserId(String userId) {
        return sessionToUserId.entrySet().stream()
                .filter(entry -> userId.equals(entry.getValue()))
                .map(entry -> entry.getKey())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 특정 방의 세션 조회
     */
    public Set<String> getSessionsByRoomId(String roomId) {
        return sessionToRoomId.entrySet().stream()
                .filter(entry -> roomId.equals(entry.getValue()))
                .map(entry -> entry.getKey())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 세션 조회
     */
    public WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 전체 통계
     */
    public WebSocketStats getStats() {
        return WebSocketStats.builder()
                .totalSessions(activeSessions.size())
                .totalUsers(sessionToUserId.values().stream().collect(java.util.stream.Collectors.toSet()).size())
                .totalRooms(sessionToRoomId.values().stream().collect(java.util.stream.Collectors.toSet()).size())
                .build();
    }
}
