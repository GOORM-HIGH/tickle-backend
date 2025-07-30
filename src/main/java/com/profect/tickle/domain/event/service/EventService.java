package com.profect.tickle.domain.event.service;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;

public interface EventService {
    CouponResponseDto createCouponEvent(CouponResponseDto request);
}
