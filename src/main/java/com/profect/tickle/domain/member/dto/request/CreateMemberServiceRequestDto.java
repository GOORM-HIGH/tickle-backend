package com.profect.tickle.domain.member.dto.request;

import com.profect.tickle.domain.member.entity.MemberRole;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateMemberServiceRequestDto(
        String email,
        String password,
        Instant birthday,
        String nickname,
        String img,
        String phoneNumber,
        MemberRole role,

        String hostBizNumber,
        String hostBizCeoName,
        String hostBizName,
        String hostBizAddress,
        String hostBizEcommerceRegistrationNumber,
        String hostBizBankName,
        String hostBizDepositor,
        String hostBizBankNumber,
        BigDecimal hostContractCharge
) {
}
