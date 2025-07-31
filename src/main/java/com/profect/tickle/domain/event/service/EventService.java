package com.profect.tickle.domain.event.service;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.dto.response.EventListResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketEventResponseDto;
import com.profect.tickle.domain.event.entity.EventType;
import com.profect.tickle.global.paging.PagingResponse;

public interface EventService {
    CouponResponseDto createCouponEvent(CouponCreateRequestDto request);

    TicketEventResponseDto createTicketEvent(TicketEventCreateRequestDto request);

    TicketApplyResponseDto applyTicketEvent(Long eventId);

    PagingResponse<EventListResponseDto> getEventList(EventType type, int page, int size);

    void issueCoupon(Long eventId);
}
