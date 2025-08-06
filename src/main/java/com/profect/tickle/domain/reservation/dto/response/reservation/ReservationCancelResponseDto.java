package com.profect.tickle.domain.reservation.dto.response.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ReservationCancelResponseDto {
    private boolean success;
    private String message;
    private Integer refundAmount;               // 환불 금액
    private Instant cancelledAt;                // 취소 일시
    
    public static ReservationCancelResponseDto success(Integer refundAmount) {
        return ReservationCancelResponseDto.builder()
                .success(true)
                .message("예매가 취소되었습니다.")
                .refundAmount(refundAmount)
                .cancelledAt(Instant.now())
                .build();
    }
    
    public static ReservationCancelResponseDto failure(String message) {
        return ReservationCancelResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
}
