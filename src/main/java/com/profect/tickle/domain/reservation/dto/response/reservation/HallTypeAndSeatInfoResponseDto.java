package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.profect.tickle.domain.performance.entity.HallType;
import java.util.List;

public record HallTypeAndSeatInfoResponseDto(
        HallType hallType,
        List<SeatInfoResponseDto> seats
) {

}
