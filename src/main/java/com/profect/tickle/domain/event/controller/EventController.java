package com.profect.tickle.domain.event.controller;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/coupon")
    @Operation(summary = "할인쿠폰 이벤트 생성", description = "관리자가 할인쿠폰 이벤트를 생성합니다.")
    @ApiResponse(responseCode = "201", description = "쿠폰 생성 성공")
    public ResultResponse<?> createCoupon(@RequestBody CouponResponseDto request) {
        CouponResponseDto response =  eventService.createCouponEvent(request);

        return ResultResponse.of(ResultCode.EVENT_CREATE_SUCCESS, response);
    }

}