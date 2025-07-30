package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Coupon;

public record CouponListResponseDto (
        Long couponId,
        String couponName,
        Short couponRate
) implements EventListResponseDto {
    public static CouponListResponseDto from(Coupon coupon) {
        return new CouponListResponseDto(
                coupon.getId(),
                coupon.getName(),
                coupon.getRate()
        );
    }
}