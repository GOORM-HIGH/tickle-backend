package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.mapper.ChatMessageMapper;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 읽지 않은 메시지 개수 최적화 서비스
 * 
 * 주요 기능:
 * 1. 읽지 않은 메시지 개수 Redis 캐싱
 * 2. DB 부하 감소로 처리량 향상
 * 3. 실시간 업데이트 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisUnreadCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatParticipantsRepository chatParticipantsRepository;

    private static final String UNREAD_COUNT_KEY = "unread_count:chat_room:%d:member:%d";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5); // 5분 캐시

    /**
     * 읽지 않은 메시지 개수 조회 (Redis 캐싱)
     * 
     * @param chatRoomId 채팅방 ID
     * @param memberId 회원 ID
     * @return 읽지 않은 메시지 개수
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(Long chatRoomId, Long memberId) {
        try {
            String cacheKey = String.format(UNREAD_COUNT_KEY, chatRoomId, memberId);
            
            // 1. Redis에서 캐시된 값 조회
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                int unreadCount = Integer.parseInt(cachedValue.toString());
                log.debug("Redis에서 읽지 않은 메시지 개수 조회: chatRoomId={}, memberId={}, unreadCount={}", 
                        chatRoomId, memberId, unreadCount);
                return unreadCount;
            }

            // 2. Redis에 없으면 DB에서 조회
            int unreadCount = getUnreadCountFromDB(chatRoomId, memberId);
            
            // 3. Redis에 캐싱
            redisTemplate.opsForValue().set(cacheKey, unreadCount, CACHE_TTL);
            
            log.info("DB에서 읽지 않은 메시지 개수 조회 후 Redis 캐싱: chatRoomId={}, memberId={}, unreadCount={}", 
                    chatRoomId, memberId, unreadCount);
            
            return unreadCount;

        } catch (Exception e) {
            log.error("읽지않은 메시지 개수 조회 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, memberId, e.getMessage(), e);
            // Redis 오류 시 DB에서 직접 조회
            return getUnreadCountFromDB(chatRoomId, memberId);
        }
    }

    /**
     * 읽지 않은 메시지 개수 증가 (새 메시지 도착 시)
     * 
     * @param chatRoomId 채팅방 ID
     * @param memberId 회원 ID (발신자 제외)
     */
    public void incrementUnreadCount(Long chatRoomId, Long memberId) {
        try {
            String cacheKey = String.format(UNREAD_COUNT_KEY, chatRoomId, memberId);
            
            // Redis에서 현재 값 조회 후 증가
            Object currentValue = redisTemplate.opsForValue().get(cacheKey);
            int currentCount = currentValue != null ? Integer.parseInt(currentValue.toString()) : 0;
            int newCount = currentCount + 1;
            
            // Redis에 업데이트된 값 저장
            redisTemplate.opsForValue().set(cacheKey, newCount, CACHE_TTL);
            
            log.debug("읽지 않은 메시지 개수 증가: chatRoomId={}, memberId={}, count={} -> {}", 
                    chatRoomId, memberId, currentCount, newCount);

        } catch (Exception e) {
            log.error("읽지 않은 메시지 개수 증가 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, memberId, e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 메시지 개수 초기화 (메시지 읽음 처리 시)
     * 
     * @param chatRoomId 채팅방 ID
     * @param memberId 회원 ID
     */
    @CacheEvict(value = "unreadCount", key = "#chatRoomId + ':' + #memberId")
    public void resetUnreadCount(Long chatRoomId, Long memberId) {
        try {
            String cacheKey = String.format(UNREAD_COUNT_KEY, chatRoomId, memberId);
            redisTemplate.delete(cacheKey);
            
            log.info("읽지 않은 메시지 개수 초기화: chatRoomId={}, memberId={}", chatRoomId, memberId);

        } catch (Exception e) {
            log.error("읽지 않은 메시지 개수 초기화 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, memberId, e.getMessage(), e);
        }
    }

    /**
     * DB에서 읽지 않은 메시지 개수 조회
     */
    private int getUnreadCountFromDB(Long chatRoomId, Long memberId) {
        try {
            // 1. 사용자 및 채팅방 유효성 검사 (간단한 구현)
            // 실제 구현에서는 별도 검증 로직 필요
            
            // 2. 마지막 읽은 메시지 ID 조회 (간단한 구현)
            Long lastReadMessageId = 0L; // 간단한 구현

            // 3. 읽지 않은 메시지 개수 조회
            int unreadCount = chatMessageMapper.countUnreadMessages(chatRoomId, memberId, lastReadMessageId);

            log.debug("DB에서 읽지 않은 메시지 개수 조회: chatRoomId={}, memberId={}, unreadCount={}", 
                    chatRoomId, memberId, unreadCount);

            return unreadCount;

        } catch (Exception e) {
            log.error("DB에서 읽지 않은 메시지 개수 조회 중 오류 발생: chatRoomId={}, memberId={}, error={}", 
                    chatRoomId, memberId, e.getMessage(), e);
            throw new BusinessException("읽지 않은 메시지 개수 조회 중 오류 발생", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
