package com.profect.tickle.domain.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MailCreateServiceRequestDto(
        @NotNull @Email String to,
        @NotBlank @Size(max = 100) String subject,
        @NotBlank @Size(max = 255) String content
) {
}
