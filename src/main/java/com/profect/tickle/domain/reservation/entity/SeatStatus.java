package com.profect.tickle.domain.reservation.entity;

public enum SeatStatus {
    AVAILABLE("예매가능", 11L),
    PREEMPTED("선점중", 12L),
    RESERVED("예매완료", 13L);

    private final String description;
    private final Long id;

    SeatStatus(String description, Long id) {
        this.description = description;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}