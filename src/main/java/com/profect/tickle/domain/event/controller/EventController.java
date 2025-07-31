package com.profect.tickle.domain.event.controller;

import com.profect.tickle.domain.event.dto.response.TicketApplyResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketEventResponseDto;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/coupon")
    public ResultResponse<CouponResponseDto> createCoupon(@Valid @RequestBody CouponCreateRequestDto request) {
        CouponResponseDto response = eventService.createCouponEvent(request);
        return ResultResponse.of(ResultCode.EVENT_CREATE_SUCCESS, response);
    }

    @PostMapping("/ticket")
    public ResultResponse<TicketEventResponseDto> createTicketEvent(@Valid @RequestBody TicketEventCreateRequestDto request) {
        TicketEventResponseDto response = eventService.createTicketEvent(request);
        return ResultResponse.of(ResultCode.EVENT_CREATE_SUCCESS, response);
    }

    @PostMapping("/ticket/{eventId}")
    public ResultResponse<TicketApplyResponseDto> applyTicketEvent(@PathVariable Long eventId) {
        TicketApplyResponseDto response = eventService.applyTicketEvent(eventId);
        return ResultResponse.of(ResultCode.EVENT_CREATE_SUCCESS, response);
    }

    @PostMapping("/coupon/{eventId}")
    public ResultResponse<String> issueCoupon(@PathVariable Long eventId) {
        eventService.issueCoupon(eventId);
        return ResultResponse.of(ResultCode.COUPON_ISSUE_SUCCESS, "[이벤트 쿠폰 지급 완료]: eventId = " + eventId);
    }
}