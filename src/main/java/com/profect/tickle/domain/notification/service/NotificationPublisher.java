package com.profect.tickle.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final StreamOperations<String, String, Object> streamOperations;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("#{@notificationStreamKey}")
    private String notificationStreamKey;  // "notification-stream"

    private static final String FAILED_NOTIFICATIONS_KEY = "failed-notifications";

    // 알림 발행 (자동 재시도 포함)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 2000,      // 2초 대기
                    multiplier = 2.0,  // 지수적 백오프 (2초 → 4초 → 8초)
                    maxDelay = 10000   // 최대 10초
            )
    )
    public void publishNotification(NotificationEnvelope<?> message) {
        try {
            Map<String, Object> streamData = Map.of(
                    "type", message.type().name(),
                    "subject", message.subject(),
                    "content", message.content(),
                    "createdAt", message.createdAt().toString(),
                    "link", message.link() != null ? message.link() : "",
                    "data", message.data() != null ? message.data() : Map.of()
            );

            RecordId recordId = streamOperations.add(
                    StreamRecords.newRecord()
                            .ofMap(streamData)
                            .withStreamKey(notificationStreamKey)
            );

            log.info("알림 Stream 발행 성공: recordId={}, type={}", recordId, message.type());
        } catch (Exception e) {
            log.warn("알림 Stream 발행 시도 실패: type={}, subject={}",
                    message.type(), message.subject(), e);
            throw e; // 재시도를 위해 예외 재발생
        }
    }

    // 모든 재시도 실패 시 호출되는 복구 메서드
    @Recover
    public void recover(Exception ex, NotificationEnvelope<?> message) {
        log.error("알림 Stream 발행 최종 실패 - 모든 재시도 소진: type={}, subject={}",
                message.type(), message.subject(), ex);

        // 실패한 알림을 별도 저장소에 보관
        handleFailedNotification(message, ex);
    }

    // 실패한 알림 처리 - Redis List 구현
    private void handleFailedNotification(NotificationEnvelope<?> message, Exception ex) {
        try {
            // NotificationEnvelope를 JSON으로 직렬화
            String messageJson = objectMapper.writeValueAsString(message);

            // 실패 정보와 함께 저장할 데이터 구성
            Map<String, Object> failedData = Map.of(
                    "message", messageJson,
                    "failedAt", Instant.now().toString(),
                    "errorMessage", ex.getMessage(),
                    "retryCount", 3  // 최대 재시도 횟수
            );

            // Redis List에 실패한 알림 저장 (왼쪽에 추가 - LIFO 방식)
            redisTemplate.opsForList().leftPush(FAILED_NOTIFICATIONS_KEY, failedData);

            log.info("실패한 알림을 Redis List에 저장 완료: type={}, key={}",
                    message.type(), FAILED_NOTIFICATIONS_KEY);

        } catch (Exception e) {
            log.error("실패한 알림 저장 중 오류 발생: type={}", message.type(), e);
        }
    }

    // 실패한 알림 목록 조회 (관리용)
    public long getFailedNotificationCount() {
        Long size = redisTemplate.opsForList().size(FAILED_NOTIFICATIONS_KEY);
        return size != null ? size : 0L;
    }

    // 실패한 알림 목록 조회 (최신 N개)
    public java.util.List<Object> getRecentFailedNotifications(int count) {
        return redisTemplate.opsForList().range(FAILED_NOTIFICATIONS_KEY, 0, count - 1);
    }

    // 실패한 알림 재처리 (수동)
    public boolean retryFailedNotification() {
        try {
            Object failedData = redisTemplate.opsForList().rightPop(FAILED_NOTIFICATIONS_KEY);
            if (failedData != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> failedMap = (Map<String, Object>) failedData;
                String messageJson = (String) failedMap.get("message");

                NotificationEnvelope<?> message = objectMapper.readValue(messageJson, NotificationEnvelope.class);
                publishNotification(message); // 재시도

                log.info("실패한 알림 재처리 완료: type={}", message.type());
                return true;
            }
        } catch (Exception e) {
            log.error("실패한 알림 재처리 중 오류", e);
        }
        return false;
    }
}
