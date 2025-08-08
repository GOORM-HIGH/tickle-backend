package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.profect.tickle.domain.reservation.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservedSeatDto {
    private Long seatId;
    private String seatNumber;
    private SeatGrade seatGrade;
    private Integer seatPrice;
    private String seatCode;
}
