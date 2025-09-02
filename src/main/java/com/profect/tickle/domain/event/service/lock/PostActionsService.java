package com.profect.tickle.domain.event.service.lock;

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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.hibernate.query.sqm.tree.SqmNode.log;

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
    private final EntityManager entityManager;

    // (락 밖) 포인트 내역 저장
    @Transactional
    public void recordPointHistory(Long memberId, int amount, PointTarget target) {
        Member member = memberRepository.getReferenceById(memberId);
        var p = Point.deduct(member, amount, target);
        pointRepository.save(p);

        // 반드시 DB에 추가되게끔 해보자.
        entityManager.flush();
        log.info("[recordPointHistory] flushed id={}", p.getId());
    }

    // (락 밖) 당첨자만 좌석 점유 + 예약 생성 (조건부 UPDATE 한 방)
    @Transactional
    public void reserveSeatAndCreateReservation(Long seatId, Long memberId, int accrued) {
        final Long RESERVED = statusProvider.provide(StatusIds.Seat.RESERVED).getId();
        final Long AVAILABLE = statusProvider.provide(StatusIds.Seat.AVAILABLE).getId();

        int updated = seatRepository.tryReserveSeat(seatId, memberId, RESERVED, AVAILABLE);
        if (updated == 0) return; // 경합에서 졌으면 조용히 종료

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
