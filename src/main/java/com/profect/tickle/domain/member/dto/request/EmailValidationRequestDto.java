package com.profect.tickle.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailValidationRequestDto(
        @Schema(description = "이메일 주소", example = "test@example.com")
        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email,

        @Schema(description = "인증코드", example = "A1B2C3D4")
        @NotBlank(message = "인증코드를 입력해주세요.")
        String code
) {
}
