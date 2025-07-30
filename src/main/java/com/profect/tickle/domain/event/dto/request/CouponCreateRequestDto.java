package com.profect.tickle.domain.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "할인쿠폰 이벤트 생성 요청 DTO")
public record CouponCreateRequestDto(
        @Schema(description = "쿠폰 이름", example = "여름 할인 쿠폰")
        String couponName,

        @Schema(description = "발급 가능한 쿠폰 수량", example = "300")
        short couponCount,

        @Schema(description = "할인율 (%)", example = "30")
        short couponRate,

        @Schema(description = "유효기간 (만료일)", example = "2025-08-31")
        LocalDate couponValid
) {}