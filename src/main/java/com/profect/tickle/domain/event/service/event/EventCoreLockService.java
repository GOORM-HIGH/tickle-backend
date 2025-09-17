package com.profect.tickle.domain.event.service.event;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCoreLockService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final StatusProvider statusProvider;
    private final ReservationService reservationService;

    //TODO: 임계영역에 대해서 동시성을 보장하면 원하는 결과가 나올겁니다?
    //TODO: 영한님의 고급 1편을 보세요. 자바 코드에 대한 동시성을 찾아보세요

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventDecision applyCore(Long eventId, Long memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        Long statusId = event.getStatus().getId();

        if (!StatusIds.Event.IN_PROGRESS.equals(statusId)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }

        // 2) 포인트 차감
        short delta = event.getPerPrice();
        var deducted = memberRepository.tryDeductPointReturning(memberId, delta);
        if (deducted.isEmpty()) throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);

        // 3) 누적 + 종료 전환(원자)
        var rows = eventRepository.accrueAndMaybeComplete(
                eventId, delta,
                StatusIds.Event.IN_PROGRESS,
                StatusIds.Event.COMPLETED);

        if (rows.isEmpty()) throw new BusinessException(ErrorCode.EVENT_ALREADY_COMPLETED);

        var r = rows.get(0);
        boolean completed = r.getStatusId().equals(StatusIds.Event.COMPLETED);

        Long seatId = null;
        boolean winner = false;

        if (completed) {
            seatId = reservationService.assignSeatForWinner(eventId, memberId);
            System.out.println("seatId = " + seatId);
            winner = (seatId != null);  // ★ 좌석 배정 성공 시에만 winner
        }

        return new EventDecision(eventId, memberId, delta, winner, r.getEventAccrued(), seatId);
    }
}