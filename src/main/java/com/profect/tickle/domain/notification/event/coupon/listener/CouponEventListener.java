package com.profect.tickle.domain.notification.event.coupon.listener;

import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import com.profect.tickle.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 관련 이벤트 리스너
 *
 * - 현재는 쿠폰 만료 임박 시 알림을 발송하는 기능만 담당
 * - 향후 다른 쿠폰 이벤트(예: 쿠폰 발급, 사용 등)도 이 리스너에서 처리 가능
 */
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
