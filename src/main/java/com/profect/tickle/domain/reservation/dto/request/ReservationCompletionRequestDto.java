package com.profect.tickle.domain.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCompletionRequestDto {

    @NotBlank
    private String preemptionToken;     // 선점 토큰

    @NotNull
    @Min(1)
    private Integer totalAmount;        // 총 결제 금액

    private Long couponId;              // 사용할 쿠폰 ID (선택사항)
}