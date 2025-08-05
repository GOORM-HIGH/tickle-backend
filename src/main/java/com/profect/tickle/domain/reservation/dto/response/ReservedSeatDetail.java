package com.profect.tickle.domain.reservation.dto.response;

import com.profect.tickle.domain.reservation.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservedSeatDetail {
    private Long seatId;
    private String seatNumber;                  // 좌석 번호
    private SeatGrade seatGrade;               // 좌석 등급
    private Integer seatPrice;                  // 좌석 가격
}
