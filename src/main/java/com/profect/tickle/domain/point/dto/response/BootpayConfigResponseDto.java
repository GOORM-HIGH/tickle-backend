package com.profect.tickle.domain.point.dto.response;

import com.profect.tickle.domain.member.entity.Member;
import lombok.Builder;

@Builder
public record BootpayConfigResponseDto(
        String appId,
        String orderId,
        String username,
        String email,
        String phone
) {
    public static BootpayConfigResponseDto from(String applicationId,
                                                String orderId,
                                                Member member) {
        return new BootpayConfigResponseDto(
                applicationId,
                orderId,
                member.getNickname(),
                member.getEmail(),
                member.getPhoneNumber()
        );
    }
}