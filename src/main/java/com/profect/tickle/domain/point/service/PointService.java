package com.profect.tickle.domain.point.service;

import com.profect.tickle.domain.point.dto.request.ChargePointRequestDto;
import com.profect.tickle.domain.point.dto.response.PointResponseDto;
import com.profect.tickle.domain.point.dto.response.PointSimpleResponseDto;
import com.profect.tickle.global.paging.PagingResponse;

public interface PointService {

    PointResponseDto charge(ChargePointRequestDto request);

    PointSimpleResponseDto getCurrentPoint();

    PagingResponse<PointSimpleResponseDto> getPointHistory(int page, int size);
}


