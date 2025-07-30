package com.profect.tickle.domain.event.controller;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.dto.response.EventListResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/coupon")
    public ResultResponse<?> createCoupon(@Valid @RequestBody CouponCreateRequestDto request) {
        CouponResponseDto response = eventService.createCouponEvent(request);
        return ResultResponse.of(ResultCode.EVENT_CREATE_SUCCESS, response);
    }
}