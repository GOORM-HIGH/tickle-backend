package com.profect.tickle.domain.notification.event.coupon;

import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.member.entity.Member;

import java.time.Duration;

public record CouponAlmostExpiredEvent(
        Member member,       // 쿠폰을 보유한 사용자
        Coupon coupon,       // 만료 임박 쿠폰
        Duration remaining   // 남은 시간
) {
}
