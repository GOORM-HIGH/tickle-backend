package com.profect.tickle.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class PaymentInfo {
    private Integer price;                      // 결제 금액
    private Instant paidAt;                     // 결제 일시
}
