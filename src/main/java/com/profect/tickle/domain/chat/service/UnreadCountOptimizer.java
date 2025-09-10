package com.profect.tickle.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 읽지 않은 메시지 개수 최적화 서비스 (Redis 활용)
 * 
 * 성능 최적화 포인트:
 * 1. DB 조회 대신 Redis에서 실시간 카운트 관리
 * 2. 배치 업데이트로 DB 부하 감소
 * 3. 메모리 기반 빠른 응답
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCountOptimizer {

    private final RedisTemplate<String, String> customStringRedisTemplate;
    
    // Redis 키 패턴
    private static final String UNREAD_COUNT_KEY = "unread:count:%d:%d"; // unread:count:roomId:memberId
    private static final String UNREAD_ROOMS_KEY = "unread:rooms:%d";    // unread:rooms:memberId
    private static final String MESSAGE_TIMESTAMP_KEY = "message:timestamp:%d:%d"; // message:timestamp:roomId:memberId
    
    // TTL 설정 (24시간)
    private static final Duration TTL = Duration.ofHours(24);

    /**
     * 읽지 않은 메시지 개수 증가
     * 
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     * @param increment 증가할 개수 (기본 1)
     */
    public void incrementUnreadCount(Long roomId, Long memberId, int increment) {
        try {
            String countKey = String.format(UNREAD_COUNT_KEY, roomId, memberId);
            String roomsKey = String.format(UNREAD_ROOMS_KEY, memberId);
            
            // Redis에서 원자적 증가 연산
            Long newCount = customStringRedisTemplate.opsForValue().increment(countKey, increment);
            
            // TTL 설정 (24시간)
            customStringRedisTemplate.expire(countKey, TTL);
            
            // 사용자가 읽지 않은 채팅방 목록에 추가
            customStringRedisTemplate.opsForSet().add(roomsKey, roomId.toString());
            customStringRedisTemplate.expire(roomsKey, TTL);
            
            log.debug("읽지 않은 메시지 개수 증가: roomId={}, memberId={}, increment={}, newCount={}", 
                    roomId, memberId, increment, newCount);
                    
        } catch (Exception e) {
            log.error("읽지 않은 메시지 개수 증가 실패: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 메시지 개수 조회
     * 
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     * @return 읽지 않은 메시지 개수
     */
    public int getUnreadCount(Long roomId, Long memberId) {
        try {
            String countKey = String.format(UNREAD_COUNT_KEY, roomId, memberId);
            String count = customStringRedisTemplate.opsForValue().get(countKey);
            
            int unreadCount = count != null ? Integer.parseInt(count) : 0;
            
            log.debug("읽지 않은 메시지 개수 조회: roomId={}, memberId={}, count={}", 
                    roomId, memberId, unreadCount);
                    
            return unreadCount;
            
        } catch (Exception e) {
            log.error("읽지 않은 메시지 개수 조회 실패: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 읽지 않은 메시지 개수 초기화 (읽음 처리)
     * 
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     */
    public void resetUnreadCount(Long roomId, Long memberId) {
        try {
            String countKey = String.format(UNREAD_COUNT_KEY, roomId, memberId);
            String roomsKey = String.format(UNREAD_ROOMS_KEY, memberId);
            
            // 읽지 않은 메시지 개수 삭제
            customStringRedisTemplate.delete(countKey);
            
            // 사용자의 읽지 않은 채팅방 목록에서 제거
            customStringRedisTemplate.opsForSet().remove(roomsKey, roomId.toString());
            
            log.debug("읽지 않은 메시지 개수 초기화: roomId={}, memberId={}", roomId, memberId);
            
        } catch (Exception e) {
            log.error("읽지 않은 메시지 개수 초기화 실패: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage(), e);
        }
    }

    /**
     * 사용자의 모든 읽지 않은 메시지 개수 조회
     * 
     * @param memberId 사용자 ID
     * @return 채팅방별 읽지 않은 메시지 개수 맵
     */
    public java.util.Map<Long, Integer> getAllUnreadCounts(Long memberId) {
        try {
            String roomsKey = String.format(UNREAD_ROOMS_KEY, memberId);
            Set<String> roomIds = customStringRedisTemplate.opsForSet().members(roomsKey);
            
            java.util.Map<Long, Integer> unreadCounts = new java.util.HashMap<>();
            
            if (roomIds != null) {
                for (String roomIdStr : roomIds) {
                    Long roomId = Long.parseLong(roomIdStr);
                    int count = getUnreadCount(roomId, memberId);
                    if (count > 0) {
                        unreadCounts.put(roomId, count);
                    }
                }
            }
            
            log.debug("사용자 전체 읽지 않은 메시지 개수 조회: memberId={}, counts={}", 
                    memberId, unreadCounts);
                    
            return unreadCounts;
            
        } catch (Exception e) {
            log.error("사용자 전체 읽지 않은 메시지 개수 조회 실패: memberId={}, error={}", 
                    memberId, e.getMessage(), e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * 메시지 타임스탬프 업데이트 (읽음 처리 시)
     * 
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     * @param messageId 마지막 읽은 메시지 ID
     */
    public void updateLastReadMessage(Long roomId, Long memberId, Long messageId) {
        try {
            String timestampKey = String.format(MESSAGE_TIMESTAMP_KEY, roomId, memberId);
            customStringRedisTemplate.opsForValue().set(timestampKey, messageId.toString(), TTL);
            
            log.debug("마지막 읽은 메시지 업데이트: roomId={}, memberId={}, messageId={}", 
                    roomId, memberId, messageId);
                    
        } catch (Exception e) {
            log.error("마지막 읽은 메시지 업데이트 실패: roomId={}, memberId={}, messageId={}, error={}", 
                    roomId, memberId, messageId, e.getMessage(), e);
        }
    }

    /**
     * 마지막 읽은 메시지 ID 조회
     * 
     * @param roomId 채팅방 ID
     * @param memberId 사용자 ID
     * @return 마지막 읽은 메시지 ID
     */
    public Long getLastReadMessageId(Long roomId, Long memberId) {
        try {
            String timestampKey = String.format(MESSAGE_TIMESTAMP_KEY, roomId, memberId);
            String messageIdStr = customStringRedisTemplate.opsForValue().get(timestampKey);
            
            Long messageId = messageIdStr != null ? Long.parseLong(messageIdStr) : 0L;
            
            log.debug("마지막 읽은 메시지 ID 조회: roomId={}, memberId={}, messageId={}", 
                    roomId, memberId, messageId);
                    
            return messageId;
            
        } catch (Exception e) {
            log.error("마지막 읽은 메시지 ID 조회 실패: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Redis 캐시 초기화 (테스트용)
     */
    public void clearCache() {
        try {
            Set<String> keys = customStringRedisTemplate.keys("unread:*");
            if (keys != null && !keys.isEmpty()) {
                customStringRedisTemplate.delete(keys);
            }
            
            Set<String> messageKeys = customStringRedisTemplate.keys("message:timestamp:*");
            if (messageKeys != null && !messageKeys.isEmpty()) {
                customStringRedisTemplate.delete(messageKeys);
            }
            
            log.info("Redis 캐시 초기화 완료");
            
        } catch (Exception e) {
            log.error("Redis 캐시 초기화 실패: error={}", e.getMessage(), e);
        }
    }
}
