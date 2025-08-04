package com.profect.tickle.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSseResponseDto {

    private String title;
    private String message;
}
