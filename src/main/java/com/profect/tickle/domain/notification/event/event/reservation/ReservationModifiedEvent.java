package com.profect.tickle.domain.notification.event.event.reservation;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.reservation.entity.Reservation;

public record ReservationModifiedEvent(
        Performance performance, // 공연 정보
        Member member, // 예매 유저
        Reservation reservation // 예매 정보
) {
}
