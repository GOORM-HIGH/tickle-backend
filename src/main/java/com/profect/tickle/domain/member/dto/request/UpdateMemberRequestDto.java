package com.profect.tickle.domain.member.dto.request;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateMemberRequestDto {

    String nickname;
    BigDecimal charge;
}
