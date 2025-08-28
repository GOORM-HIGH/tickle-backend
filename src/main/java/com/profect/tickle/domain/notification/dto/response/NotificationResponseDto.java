package com.profect.tickle.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponseDto {

    private Long id;
    private String title;
    private String content;
    private boolean isRead;
    private Instant createdAt;
}
