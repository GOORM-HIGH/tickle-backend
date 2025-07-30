package com.profect.tickle.domain.event.service.impl;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CouponResponseDto createCouponEvent(CouponResponseDto request) {
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
