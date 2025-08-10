package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Coupon;

import java.time.Instant;

public record CouponListResponseDto (
        Long id,
        String name,
        Short rate,
        Long eventId,
        Instant validDate
) implements EventListResponseDto {
    @Override
    public Long getEventId() {
        return eventId;
    }
    @Override
    public String getName() {
        return name;
    }
}