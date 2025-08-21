package com.profect.tickle.domain.notification.event.coupon.event;

import java.time.Instant;

public record CouponAlmostExpiredEvent(
        Long memberId,
        String memberEmail,       // 쿠폰을 보유한 사용자
        String couponName,       // 만료 임박 쿠폰
        Instant expiryDate   // 남은 시간
) {
}
