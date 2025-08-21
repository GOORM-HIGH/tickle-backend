package com.profect.tickle.domain.notification.event.coupon.listener;

import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import com.profect.tickle.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final NotificationService notificationService;

    // 쿠폰 만료 임박 이벤트 처리
    @Async
    @EventListener
    public void handleCouponAlmostExpired(CouponAlmostExpiredEvent event) {
        try {
            log.info("쿠폰 만료 임박 이벤트 감지: memberEmail={}, couponName={}, expiryDate={}",
                    event.memberEmail(), event.couponName(), event.expiryDate());

            notificationService.sendCouponAlmostExpiredNotification(
                    event.memberEmail(), event.couponName(), event.expiryDate());
        } catch (Exception e) {
            log.error("CouponAlmostExpiredEvent 처리 중 오류", e);
        }
    }
}
