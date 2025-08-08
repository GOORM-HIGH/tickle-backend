package com.profect.tickle.domain.notification.event.reservation.event;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDto;
import com.profect.tickle.domain.reservation.entity.Reservation;

import java.util.List;

public record PerformanceModifiedEvent(
        PerformanceDto performance, // 공연 정보
        List<ReservationDto> reservationList, // 예매 정보
        Member member // 예매 유저
) {
}
