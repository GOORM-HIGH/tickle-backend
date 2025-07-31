package com.profect.tickle.domain.event.controller;

import com.profect.tickle.domain.event.dto.response.*;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.entity.EventType;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @GetMapping
    @Operation(summary = "이벤트 목록 조회", description = "쿠폰 또는 티켓 이벤트를 페이징으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PagingResponse.class)))
    public ResultResponse<PagingResponse<EventListResponseDto>> getEventList(@RequestParam("type") EventType eventType,
                                                                                       @RequestParam("page") int page,
                                                                                       @RequestParam("size") int size) {
        PagingResponse<EventListResponseDto> response = eventService.getEventList(eventType, page, size);

        return ResultResponse.of(ResultCode.EVENT_INFO_SUCCESS, response);
    }

    @GetMapping("/ticket/{eventId}")
    @Operation(summary = "티켓 이벤트 상세 조회")
    public ResultResponse<TicketEventDetailResponseDto> getTicketEventDetail(@PathVariable Long eventId) {
        TicketEventDetailResponseDto detail = eventService.getTicketEventDetail(eventId);
        return ResultResponse.of(ResultCode.EVENT_INFO_SUCCESS, detail);
    }

    @GetMapping("/ticket/search")
    @Operation(summary = "티켓 이벤트 키워드 검색")
    public ResultResponse<PagingResponse<TicketListResponseDto>> searchTicketEvents(
            @RequestParam String keyword,
            @RequestParam int page,
            @RequestParam int size
    ) {
        PagingResponse<TicketListResponseDto> response = eventService.searchTicketEvents(keyword, page, size);
        return ResultResponse.of(ResultCode.EVENT_INFO_SUCCESS, response);
    }

}