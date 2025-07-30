package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CouponResponseDto createCouponEvent(CouponCreateRequestDto request) {
        if (couponRepository.existsByName(request.couponName()))
            throw new BusinessException(ErrorCode.DUPLICATE_COUPON_NAME);

        Coupon coupon = Coupon.create(
                request.couponName(),
                request.couponCount(),
                request.couponRate(),
                request.couponValid()
        );
        couponRepository.save(coupon);

        return CouponResponseDto.from(coupon);
    }
}
