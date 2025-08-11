package com.profect.tickle.domain.reservation.dto.response.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ReservationHistoryResponseDto {
    private Long reservationId;
    private String reservationNumber;           // 예매 번호
    private String performanceTitle;            // 공연명
    private String performanceHall;            // 공연장
    private Instant performanceDate;      // 공연 일시
    private Integer seatCount;                  // 예매 좌석 수
    private List<String> seatNumbers;          // 좌석 번호들 (간단히)
    private Integer price;                      // 금액
    private String status;                      // 예매 상태
    private Instant reservedAt;                 // 예매 일시
    private boolean cancellable;                // 취소 가능 여부
}
