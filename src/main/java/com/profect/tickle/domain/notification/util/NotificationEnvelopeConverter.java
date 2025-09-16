package com.profect.tickle.domain.notification.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEnvelopeConverter {

    private final ObjectMapper objectMapper;

    /**
     * Redis Stream 메시지 데이터를 NotificationEnvelope로 변환
     */
    public NotificationEnvelope<Object> convertToNotificationEnvelope(Map<String, Object> recordValue) {
        try {
            // 필수 필드 검증
            String typeStr = (String) recordValue.get("type");
            if (typeStr == null || typeStr.trim().isEmpty()) {
                throw new IllegalArgumentException("type 필드가 누락되었습니다");
            }

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

            // receivedMemberId 필드 추출
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
            String subject = (String) recordValue.getOrDefault("subject", "");
            String content = (String) recordValue.getOrDefault("content", "");

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
