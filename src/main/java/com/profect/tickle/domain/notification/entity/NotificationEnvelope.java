package com.profect.tickle.domain.notification.entity;

import java.time.Instant;

public record NotificationEnvelope<T>(
        NotificationKind type,
        String subject,
        String content,
        Instant createdAt,
        String link,   // 옵션
        T data         // 타입별 상세
) {
}
