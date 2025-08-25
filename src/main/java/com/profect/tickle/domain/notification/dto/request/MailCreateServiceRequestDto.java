package com.profect.tickle.domain.notification.dto.request;

public record MailCreateServiceRequestDto(
        String to,
        String subject,
        String content
) {
}
