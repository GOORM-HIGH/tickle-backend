package com.profect.tickle.domain.event.service.event;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
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
    private final StatusProvider statusProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventDecision applyCore(Long eventId, Long memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        //TODO: 임계영역에 대해서 동시성을 보장하면 원하는 결과가 나올겁니다?
        //TODO: 영한님의 고급 1편을 보세요. 자바 코드에 대한 동시성을 찾아보세요
        if (!StatusIds.Event.IN_PROGRESS.equals(event.getStatus().getId())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }

        short perPrice = event.getPerPrice();
        int memberPoint = memberRepository.tryDeductPoint(memberId, perPrice);
        if (memberPoint == 0) throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);

        event.accumulate(perPrice);

        boolean winner = event.getAccrued() >= event.getGoalPrice();
        Long seatId = null;
        if (winner) {
            event.updateStatus(statusProvider.provide(StatusIds.Event.COMPLETED));
            seatId = event.getSeat().getId();
        }

        return new EventDecision(event.getId(), memberId, perPrice, winner, event.getAccrued(), seatId);
    }
}