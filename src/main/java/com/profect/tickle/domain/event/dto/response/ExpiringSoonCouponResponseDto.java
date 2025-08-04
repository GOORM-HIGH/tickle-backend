package com.profect.tickle.domain.event.dto.response;

import java.time.LocalDate;

public record ExpiringSoonCouponResponseDto(
        String memberEmail,    // 쿠폰 소유자 이메일
        String couponName,     // 쿠폰 이름
        LocalDate expiryDate   // 쿠폰 만료일
) {
}
