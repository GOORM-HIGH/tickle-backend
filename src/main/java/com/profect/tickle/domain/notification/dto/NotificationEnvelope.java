package com.profect.tickle.domain.notification.dto;

import com.profect.tickle.domain.notification.entity.NotificationKind;

import java.time.Instant;

public record NotificationEnvelope<T>(
        NotificationKind type,
        Long receivedMemberId,
        String subject,
        String content,
        Instant createdAt,
        String link,
        T data
) {
}
