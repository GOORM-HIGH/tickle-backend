package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.profect.tickle.domain.reservation.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SeatInfoResponseDto {
    private Long seatId;
    private String seatNumber;        // 좌석 번호 (A1, B2 등)
    private SeatGrade seatGrade;      // 좌석 등급 (VIP, R, S)
    private Integer seatPrice;        // 좌석 가격
    private Long statusId;            // DB status ID (11: 예매가능, 12: 선점중, 13: 예매완료)
}