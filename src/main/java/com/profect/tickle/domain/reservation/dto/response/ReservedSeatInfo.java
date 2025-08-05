package com.profect.tickle.domain.reservation.dto.response;

import com.profect.tickle.domain.reservation.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservedSeatInfo {
    private Long seatId;
    private String seatNumber;
    private SeatGrade seatGrade;
    private Integer seatPrice;
}
