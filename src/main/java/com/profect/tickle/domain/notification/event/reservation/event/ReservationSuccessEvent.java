package com.profect.tickle.domain.notification.event.reservation.event;

import com.profect.tickle.domain.performance.dto.response.PerformanceServiceDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;

public record ReservationSuccessEvent(
        PerformanceServiceDto performance,  // 공연정보
        ReservationServiceDto reservation   // 예매정보
) {
}
