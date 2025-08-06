package com.profect.tickle.domain.reservation.entity;

import lombok.Getter;

public enum ReservationStatus {
    PAID("결제완료", 9L),
    CANCELED("취소됨",9L);

    @Getter
    private final String description;

    @Getter
    private final Long id;

    ReservationStatus(String description, Long id) {
        this.description = description;
        this.id = id;
    }
}
