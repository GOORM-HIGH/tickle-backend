package com.profect.tickle.domain.event.scheduler;

import com.profect.tickle.domain.event.dto.response.ExpiringSoonCouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final EventService eventService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매일 새벽 0시에 만료 1일 전 쿠폰 조회 후 이벤트 발행
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초
    public void publishExpiringSoonCoupons() {
        List<ExpiringSoonCouponResponseDto> expiringCoupons = eventService.getCouponsExpiringWithinOneDay();
        log.info("만료 임박 쿠폰 {}건 조회됨", expiringCoupons.size());

        expiringCoupons.forEach(coupon ->
                eventPublisher.publishEvent(
                        new CouponAlmostExpiredEvent(
                                coupon.memberEmail(),
                                coupon.couponName(),
                                coupon.expiryDate()
                        )
                )
        );
    }
}
