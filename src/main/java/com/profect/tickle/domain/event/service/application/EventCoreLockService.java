package com.profect.tickle.domain.event.service.application;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
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
    private final ReservationService reservationService;
    private final StatusProvider statusProvider;

    //TODO: 임계영역에 대해서 동시성을 보장하면 원하는 결과가 나올겁니다?
    //TODO: 영한님의 고급 1편을 보세요. 자바 코드에 대한 동시성을 찾아보세요

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventDecision applyCore(Long memberId, Long eventId) {

        long startTotal = System.nanoTime();
        Event event;
        long t1, t2, t3;

        // (1) Event 조회
        long start1 = System.nanoTime();
        event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        t1 = (System.nanoTime() - start1) / 1_000_000; // ms 단위

        Long statusId = event.getStatus().getId();
        if (!StatusIds.Event.IN_PROGRESS.equals(statusId)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }

        // (2) 누적 및 상태 변경 쿼리
        long start2 = System.nanoTime();
        short delta = event.getPerPrice();
        var rows = eventRepository.accrueAndMaybeComplete(
                eventId, delta,
                StatusIds.Event.IN_PROGRESS,
                StatusIds.Event.COMPLETED);
        t2 = (System.nanoTime() - start2) / 1_000_000;

        if (rows.isEmpty()) throw new BusinessException(ErrorCode.EVENT_ALREADY_COMPLETED);

        var r = rows.get(0);
        boolean completed = r.getStatusId().equals(StatusIds.Event.COMPLETED);

        // (3) 좌석 배정
        Long seatId = null;
        boolean winner = false;
        long start3 = System.nanoTime();
        if (completed) {
            seatId = reservationService.assignSeatForWinner(eventId, memberId);
            winner = (seatId != null);
        }
        t3 = (System.nanoTime() - start3) / 1_000_000;

        long total = (System.nanoTime() - startTotal) / 1_000_000;

        return new EventDecision(eventId, memberId, delta, winner, r.getEventAccrued(), seatId);
    }

    @Transactional
    public void completeEvent(Long eventId) {
        eventRepository.updateEventStatus(eventId, statusProvider.provide(StatusIds.Event.COMPLETED));
    }
}