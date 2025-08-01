package com.profect.tickle.domain.member.controller;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final EventService eventService;

    @GetMapping("/coupons")
    @Operation(summary = "내 쿠폰 목록 조회")
    public ResultResponse<PagingResponse<CouponResponseDto>> getMyCoupons(@RequestParam("page") int page,
                                                                          @RequestParam("size") int size) {
        PagingResponse<CouponResponseDto> response = eventService.getMyCoupons(page, size);
        return ResultResponse.of(ResultCode.COUPON_INFO_SUCCESS, response);
    }
}