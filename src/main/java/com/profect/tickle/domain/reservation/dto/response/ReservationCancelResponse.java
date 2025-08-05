package com.profect.tickle.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ReservationCancelResponse {
    private boolean success;
    private String message;
    private Integer refundAmount;               // 환불 금액
    private Instant cancelledAt;                // 취소 일시
    
    public static ReservationCancelResponse success(Integer refundAmount) {
        return ReservationCancelResponse.builder()
                .success(true)
                .message("예매가 취소되었습니다.")
                .refundAmount(refundAmount)
                .cancelledAt(Instant.now())
                .build();
    }
    
    public static ReservationCancelResponse failure(String message) {
        return ReservationCancelResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
