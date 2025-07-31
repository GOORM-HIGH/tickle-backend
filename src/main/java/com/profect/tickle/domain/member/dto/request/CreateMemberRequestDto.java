package com.profect.tickle.domain.member.dto.request;

import com.profect.tickle.domain.member.entity.MemberRole;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CreateMemberRequestDto {

    private String email;
    private String password;
    private LocalDate birthday;
    private String nickname;
    private String img;
    private String phoneNumber;

    private MemberRole role; // MEMBER or HOST

    // 주최자 전용
    private String hostBizNumber;
    private String hostBizCeoName;
    private String hostBizName;
    private String hostBizAddress;
    private String hostBizEcommerceRegistrationNumber;
    private String hostBizBank;
    private String hostBizDepositor;
    private String hostBizBankNumber;
}
