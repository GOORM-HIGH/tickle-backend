package com.profect.tickle.domain.event.service.application;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPreconditionService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public void validatePreApplyConditions(Long memberId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        int perPrice = event.getPerPrice();
        int currentPoint = memberRepository.findPointById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (currentPoint < perPrice)
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);

        if (!StatusIds.Event.IN_PROGRESS.equals(event.getStatus().getId()))
            throw new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);

    }
}