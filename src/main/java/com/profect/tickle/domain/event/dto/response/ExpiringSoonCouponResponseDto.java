package com.profect.tickle.domain.event.dto.response;

import java.time.Instant;

public record ExpiringSoonCouponResponseDto(
        Long memberId,          // 쿠폰 소유자 Id
        String memberEmail,     // 쿠폰 소유자 이메일
        String couponName,      // 쿠폰 이름
        Instant expiryDate      // 쿠폰 만료일
) {
}
