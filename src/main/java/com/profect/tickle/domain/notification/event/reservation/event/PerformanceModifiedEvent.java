package com.profect.tickle.domain.notification.event.reservation.event;

import com.profect.tickle.domain.performance.dto.response.PerformanceServiceDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;

import java.util.List;

public record PerformanceModifiedEvent(
        PerformanceServiceDto performance,              // 공연정보
        List<ReservationServiceDto> reservationList     // 예매정보 리스트
) {
}
