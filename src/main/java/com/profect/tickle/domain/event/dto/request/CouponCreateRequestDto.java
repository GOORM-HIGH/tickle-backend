package com.profect.tickle.domain.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "할인쿠폰 이벤트 생성 요청 DTO")
public record CouponCreateRequestDto(

        @Size(min = 2, message = "쿠폰 이름은 2자 이상이어야 합니다.")
        @Schema(description = "쿠폰 이름", example = "여름 할인 쿠폰")
        String name,

        @Size(min = 2, message = "쿠폰 내용은 2자 이상이어야 합니다.")
        @Schema(description = "쿠폰 내용", example = "첫 예매 시, 모든 공연 10% 할인")
        String content,

        @Min(value = 0, message = "쿠폰 수량은 0 이상이어야 합니다.")
        @Schema(description = "발급 가능한 쿠폰 수량", example = "300")
        Short count,

        @Min(value = 0, message = "할인율은 0 이상이어야 합니다.")
        @Schema(description = "할인율 (%)", example = "30")
        Short rate,

        @Future(message = "과거, 당일은 유효기간으로 지정될 수 없습니다.")
        @Schema(description = "유효기간 (만료일)", example = "2025-08-31")
        LocalDate valid
) {}