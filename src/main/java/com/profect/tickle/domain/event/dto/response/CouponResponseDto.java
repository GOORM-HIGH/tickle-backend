package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Coupon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "할인쿠폰 생성 응답 DTO")
public record CouponResponseDto(
        @Schema(description = "쿠폰 ID")
        Long couponId,

        @Schema(description = "쿠폰 이름")
        String couponName,

        @Schema(description = "할인율")
        short couponRate,

        @Schema(description = "수량")
        short couponCount,

        @Schema(description = "유효기간")
        LocalDate couponValid
) {
    public static CouponResponseDto from(Coupon coupon) {
        return new CouponResponseDto(
                coupon.getId(),
                coupon.getName(),
                coupon.getRate(),
                coupon.getCount(),
                coupon.getValid()
        );
    }
}