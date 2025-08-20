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
                pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]",
                timezone = "UTC"
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
    public static final String HOST_FIELDS_FORBIDDEN_MESSAGE = "MEMBER/ADMIN 권한일 때는 사업자 필드를 보낼 수 없습니다.";

    // 서비스 레이어 DTO로 변환
    public CreateMemberServiceRequestDto toServiceDto() {
        return new CreateMemberServiceRequestDto(
                email, password, birthday, nickname, img, phoneNumber, role,
                hostBizNumber, hostBizCeoName, hostBizName, hostBizAddress,
                hostBizEcommerceRegistrationNumber, hostBizBankName, hostBizDepositor,
                hostBizBankNumber, hostContractCharge
        );
    }

    // HOST면 모든 HOST 필드가 채워져 있어야 한다.
    @AssertTrue(message = HOST_FIELDS_REQUIRED_MESSAGE)
    public boolean isHostFieldsValid() {
        if (role != MemberRole.HOST) return true;
        return hasText(hostBizNumber) &&
                hasText(hostBizName) &&
                hasText(hostBizBankName) &&
                hasText(hostBizDepositor) &&
                hasText(hostBizBankNumber) &&
                hostContractCharge != null && hostContractCharge.signum() >= 0;
    }

    // HOST가 아니면(HOST 외 권한) 모든 HOST 필드는 비어 있어야 한다.
    @AssertTrue(message = HOST_FIELDS_FORBIDDEN_MESSAGE)
    public boolean isNonHostFieldsEmpty() {
        if (role == MemberRole.HOST) return true;
        return isBlank(hostBizNumber) &&
                isBlank(hostBizCeoName) &&
                isBlank(hostBizName) &&
                isBlank(hostBizAddress) &&
                isBlank(hostBizEcommerceRegistrationNumber) &&
                isBlank(hostBizBankName) &&
                isBlank(hostBizDepositor) &&
                isBlank(hostBizBankNumber) &&
                hostContractCharge == null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
