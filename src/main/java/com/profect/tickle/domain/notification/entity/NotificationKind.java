package com.profect.tickle.domain.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationKind {
    RESERVATION_SUCCESS(1L),
    PERFORMANCE_MODIFIED(2L),
    COUPON_ALMOST_EXPIRED(3L),
    AUTH_CODE_SENT(4L); // 인증번호 전송

    private final Long id;

    NotificationKind(Long id) {
        this.id = id;
    }

}
