package com.profect.tickle.domain.event.service.event;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostActionsService {

    private final PointRepository pointRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final PerformanceRepository performanceRepository;
    private final StatusProvider statusProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPointHistory(Long memberId, int amount, PointTarget target) {
        Member member = memberRepository.getReferenceById(memberId);
        Point p = Point.deduct(member, amount, target);
        pointRepository.save(p);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveSeatAndCreateReservation(Long seatId, Long memberId, int accrued) {
        final Long RESERVED = statusProvider.provide(StatusIds.Seat.RESERVED).getId();
        final Long AVAILABLE = statusProvider.provide(StatusIds.Seat.AVAILABLE).getId();

        int updated = seatRepository.tryReserveSeat(seatId, memberId, RESERVED, AVAILABLE);
        if (updated == 0) return;

        Long perfId = seatRepository.findPerformanceIdBySeatId(seatId);
        Reservation r = Reservation.create(
                memberRepository.getReferenceById(memberId),
                performanceRepository.getReferenceById(perfId),
                statusProvider.provide(StatusIds.Reservation.PAID),
                accrued
        );
        r.assignSeat(seatRepository.getReferenceById(seatId));
        reservationRepository.save(r);
    }
}