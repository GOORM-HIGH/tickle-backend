package com.profect.tickle.domain.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationKind {
    RESERVATION_SUCCESS(1L),
    PERFORMANCE_MODIFIED(2L),
    COUPON_ALMOST_EXPIRED(3L),
    AUTH_CODE_SENT(4L),
    PARTNER_PERFORMANCE_PUBLISHED(5L);

    private final Long id;

    NotificationKind(Long id) {
        this.id = id;
    }
}
