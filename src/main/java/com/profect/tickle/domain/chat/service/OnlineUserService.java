package com.profect.tickle.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnlineUserService {

    // chatRoomId -> Set<memberId>
    private final ConcurrentMap<Long, Set<Long>> onlineUsers = new ConcurrentHashMap<>();

    // sessionId -> memberId 매핑
    private final ConcurrentMap<String, Long> sessionToMember = new ConcurrentHashMap<>();

    // sessionId -> chatRoomId 매핑
    private final ConcurrentMap<String, Long> sessionToRoom = new ConcurrentHashMap<>();

    /**
     * 사용자 온라인 상태로 변경
     */
    public void addOnlineUser(String sessionId, Long chatRoomId, Long memberId) {
        log.info("사용자 온라인 추가: sessionId={}, chatRoomId={}, memberId={}", sessionId, chatRoomId, memberId);

        onlineUsers.computeIfAbsent(chatRoomId, k -> ConcurrentHashMap.newKeySet()).add(memberId);
        sessionToMember.put(sessionId, memberId);
        sessionToRoom.put(sessionId, chatRoomId);

        log.info("채팅방 {} 온라인 사용자 수: {}", chatRoomId, getOnlineCount(chatRoomId));
    }

    /**
     * 사용자 오프라인 상태로 변경
     */
    public void removeOnlineUser(String sessionId) {
        Long memberId = sessionToMember.remove(sessionId);
        Long chatRoomId = sessionToRoom.remove(sessionId);

        if (memberId != null && chatRoomId != null) {
            log.info("사용자 오프라인 처리: sessionId={}, chatRoomId={}, memberId={}", sessionId, chatRoomId, memberId);

            Set<Long> roomUsers = onlineUsers.get(chatRoomId);
            if (roomUsers != null) {
                roomUsers.remove(memberId);
                if (roomUsers.isEmpty()) {
                    onlineUsers.remove(chatRoomId);
                }
            }

            log.info("채팅방 {} 온라인 사용자 수: {}", chatRoomId, getOnlineCount(chatRoomId));
        }
    }

    /**
     * 채팅방 온라인 사용자 수 조회
     */
    public int getOnlineCount(Long chatRoomId) {
        Set<Long> users = onlineUsers.get(chatRoomId);
        return users != null ? users.size() : 0;
    }

    /**
     * 채팅방 온라인 사용자 목록 조회
     */
    public Set<Long> getOnlineUsers(Long chatRoomId) {
        return onlineUsers.getOrDefault(chatRoomId, Set.of());
    }

    /**
     * 특정 사용자가 온라인인지 확인
     */
    public boolean isUserOnline(Long chatRoomId, Long memberId) {
        Set<Long> users = onlineUsers.get(chatRoomId);
        return users != null && users.contains(memberId);
    }

    /**
     * 전체 온라인 사용자 통계
     */
    public ConcurrentMap<Long, Integer> getAllOnlineStats() {
        return onlineUsers.entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().size(),
                        (existing, replacement) -> replacement,
                        ConcurrentHashMap::new
                ));
    }
}
