package com.profect.tickle.domain.notification.event.coupon.event;

import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.member.entity.Member;

import java.time.Duration;
import java.time.LocalDate;

public record CouponAlmostExpiredEvent(
        String memberEmail,       // 쿠폰을 보유한 사용자
        String couponName,       // 만료 임박 쿠폰
        LocalDate expiryDate   // 남은 시간
) {
}
