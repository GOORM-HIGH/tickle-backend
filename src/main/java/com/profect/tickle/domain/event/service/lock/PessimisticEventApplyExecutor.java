package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PessimisticEventApplyExecutor {
    private final PointTarget eventTarget = PointTarget.EVENT;

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final PointRepository pointRepository;
    private final StatusProvider statusProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketApplyResponseDto applyTicketEventOnce(Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (!StatusIds.Event.IN_PROGRESS.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }

        Member member = getMemberOrThrow();
        Point point = member.deductPoint(event.getPerPrice(), eventTarget);
        pointRepository.save(point);

        event.accumulate(event.getPerPrice());

        boolean isWinner = (event.getAccrued() >= event.getGoalPrice());
        if (isWinner) {
            Seat seat = getSeatOrThrow(event.getSeat().getId());
            event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));

            Status paid = statusProvider.provide(StatusIds.Reservation.PAID);
            Reservation reservation = Reservation.create(member, seat.getPerformance(), paid, event.getAccrued());
            reservation.assignSeat(seat);

            Status reserved = statusProvider.provide(StatusIds.Seat.RESERVED);
            seat.completeReservation(member, reserved, null);

            reservationRepository.save(reservation);
        }

        return TicketApplyResponseDto.from(eventId, member.getId(), isWinner);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findForUpdateById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
    }

    private Seat getSeatOrThrow(Long eventSeatId) {
        return seatRepository.findById(eventSeatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }

    private Member getMemberOrThrow() {
        Long memberId = SecurityUtil.getSignInMemberId();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }


}