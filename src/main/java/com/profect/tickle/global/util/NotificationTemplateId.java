package com.profect.tickle.global.util;

public enum NotificationTemplateId {
    RESERVATION_SUCCESS(1L),
    RESERVATION_CHANGED(2L),
    RESERVATION_DELETED(3L),
    COUPON_ALMOST_EXPIRED(4L);

    private final Long id;

    NotificationTemplateId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
