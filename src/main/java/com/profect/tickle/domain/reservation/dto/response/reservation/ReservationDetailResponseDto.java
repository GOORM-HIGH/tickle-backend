package com.profect.tickle.domain.reservation.dto.response.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ReservationDetailResponseDto {
    private Long reservationId;
    private String reservationNumber;
    
    // 공연 정보
    private PerformanceInfo performance;
    
    // 좌석 정보
    private List<ReservedSeatDto> seats;
    
    // 결제 정보
    private PaymentInfo payment;
    
    // 예매 정보
    private ReservationInfo reservation;
}
