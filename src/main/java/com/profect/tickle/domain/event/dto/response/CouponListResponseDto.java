package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Coupon;

public record CouponListResponseDto (
        Long id,
        String name,
        Short rate
) implements EventListResponseDto {
    public static CouponListResponseDto from(Coupon coupon) {
        return new CouponListResponseDto(
                coupon.getId(),
                coupon.getName(),
                coupon.getRate());
    }
    @Override
    public Long getEventId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}