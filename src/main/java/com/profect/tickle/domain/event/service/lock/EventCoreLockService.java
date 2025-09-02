package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCoreLockService {
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventDecision applyCore(Long eventId, Long memberId) {
        Long seatId = null;
        Event event = eventRepository.findForUpdateById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        if (!StatusIds.Event.IN_PROGRESS.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }

        short perPrice = event.getPerPrice();

        int memberPoint = memberRepository.tryDeductPoint(memberId, perPrice);
        if (memberPoint == 0) throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);

        event.accumulate(perPrice);

        boolean winner = event.getAccrued() >= event.getGoalPrice();

        if (winner) {
            event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));
            seatId = event.getSeat().getId();
        }

        publisher.publishEvent(new TicketApplied(
                memberId,
                seatId,
                perPrice,
                event.getAccrued(),
                PointTarget.EVENT,
                winner
        ));

        return new EventDecision(event.getId(), memberId, perPrice, winner, event.getAccrued(), seatId);
    }
}
