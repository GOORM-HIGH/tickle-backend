package com.profect.tickle.domain.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.profect.tickle.domain.member.entity.MemberRole;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateMemberRequestDto(

        @Email @NotBlank String email,
        @NotBlank String password,
        @Past
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                // 날짜만 / 시간 포함(초/밀리초) 둘 다 허용
                pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]",
                timezone = "UTC" // Z가 없으면 UTC로 해석
        )
        Instant birthday,
        @NotBlank String nickname,
        String img,
        String phoneNumber,

        @NotNull MemberRole role, // MEMBER || HOST || ADMIN

        // HOST 전용
        String hostBizNumber,
        String hostBizCeoName,
        String hostBizName,
        String hostBizAddress,
        String hostBizEcommerceRegistrationNumber,
        String hostBizBankName,
        String hostBizDepositor,
        String hostBizBankNumber,
        @PositiveOrZero BigDecimal hostContractCharge
) {
    public static final String HOST_FIELDS_REQUIRED_MESSAGE = "HOST 권한일 때 사업자 필드가 모두 필요합니다.";

    // 서비스 레이어 DTO로 변환
    public CreateMemberServiceRequestDto toServiceDto() {
        return new CreateMemberServiceRequestDto(
                email, password, birthday, nickname, img, phoneNumber, role,
                hostBizNumber, hostBizCeoName, hostBizName, hostBizAddress,
                hostBizEcommerceRegistrationNumber, hostBizBankName, hostBizDepositor,
                hostBizBankNumber, hostContractCharge
        );
    }

    @AssertTrue(message = HOST_FIELDS_REQUIRED_MESSAGE)
    public boolean isHostFieldsValid() {
        if (role != MemberRole.HOST) return true;
        // 공백/빈문자열까지 체크
        return hasText(hostBizNumber) &&
                hasText(hostBizName) &&
                hasText(hostBizBankName) &&
                hasText(hostBizDepositor) &&
                hasText(hostBizBankNumber) &&
                hostContractCharge != null && hostContractCharge.signum() >= 0;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
