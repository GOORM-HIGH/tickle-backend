package com.profect.tickle.domain.point.service;


import com.profect.tickle.domain.point.dto.response.BootpayConfigResponseDto;

public interface BootPayService {

    BootpayConfigResponseDto getBootpayConfig();

    String getAccessToken();

}
