package com.profect.tickle.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailValidationCodeCreateRequest(
        @Schema(description = "이메일", example = "user@example.com")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        String email
) {
}
