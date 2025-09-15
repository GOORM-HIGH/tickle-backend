package com.profect.tickle.domain.notification.service.realtime.consumer;

import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMessageHandler {

    private final RealtimeSender sseSender;

    // Redis Stream 메시지 처리
    public boolean handleMessage(MapRecord<String, String, Object> record) {
        try {
            Map<String, Object> recordValue = record.getValue();
            log.debug("메시지 처리 시작: recordId={}, type={}", record.getId(), recordValue.get("type"));

            NotificationEnvelope<Object> envelope = convertToNotificationEnvelope(recordValue);

            return switch (envelope.type()) {
                case PARTNER_PERFORMANCE_PUBLISHED -> sseSender.sendAll(envelope);

                case RESERVATION_SUCCESS,
                     PERFORMANCE_MODIFIED,
                     COUPON_ALMOST_EXPIRED,
                     AUTH_CODE_SENT -> {
                    Long memberId = envelope.receivedMemberId();
                    yield sseSender.send(memberId, envelope);
                }

                default -> {
                    log.warn("처리되지 않는 NotificationKind: recordId={}, type={}", record.getId(), envelope.type());
                    yield false;
                }
            };

        } catch (Exception e) {
            log.error("메시지 처리 중 예외: recordId={}", record.getId(), e);
            return false;
        }
    }

    // 메시지 처리 실패 시 호출되는 콜백
    public void handleFailure(MapRecord<String, String, Object> record, Exception error) {
        log.error("메시지 처리 최종 실패: recordId={}, error={}", record.getId(), error.getMessage(), error);
    }

    // Redis Stream의 Map 데이터를 NotificationEnvelope로 변환
    private NotificationEnvelope<Object> convertToNotificationEnvelope(Map<String, Object> recordValue) {
        try {
            // 필수 필드 검증
            String typeStr = (String) recordValue.get("type");
            if (typeStr == null || typeStr.trim().isEmpty()) {
                throw new IllegalArgumentException("type 필드가 누락되었습니다");
            }

            String subject = (String) recordValue.getOrDefault("subject", "");
            String content = (String) recordValue.getOrDefault("content", "");

            String createdAtStr = (String) recordValue.get("createdAt");
            if (createdAtStr == null || createdAtStr.trim().isEmpty()) {
                throw new IllegalArgumentException("createdAt 필드가 누락되었습니다");
            }

            // 타입 변환
            NotificationKind type;
            try {
                type = NotificationKind.valueOf(typeStr.trim());
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 NotificationKind: {}, 기본값 사용", typeStr);
                type = NotificationKind.RESERVATION_SUCCESS; // 기본값
            }

            Instant createdAt;
            try {
                createdAt = Instant.parse(createdAtStr.trim());
            } catch (Exception e) {
                log.warn("createdAt 파싱 실패: {}, 현재 시간 사용", createdAtStr);
                createdAt = Instant.now();
            }

            Long receivedMemberId = null;
            Object memberIdObj = recordValue.get("receivedMemberId");
            if (memberIdObj != null) {
                try {
                    if (memberIdObj instanceof Number) {
                        receivedMemberId = ((Number) memberIdObj).longValue();
                    } else if (memberIdObj instanceof String) {
                        receivedMemberId = Long.parseLong((String) memberIdObj);
                    }
                } catch (NumberFormatException e) {
                    log.warn("receivedMemberId 파싱 실패: {}, null로 설정", memberIdObj);
                }
            }

            // 선택적 필드 처리
            String link = (String) recordValue.get("link");
            if (link != null && link.trim().isEmpty()) {
                link = null; // 빈 문자열을 null로 변환
            }

            Object data = recordValue.getOrDefault("data", Map.of());

            return new NotificationEnvelope<>(type, receivedMemberId, subject, content, createdAt, link, data);

        } catch (Exception e) {
            log.error("NotificationEnvelope 변환 실패: recordValue={}", recordValue, e);
            throw new IllegalArgumentException("NotificationEnvelope 변환 중 오류 발생", e);
        }
    }
}