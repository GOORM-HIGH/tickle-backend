package com.profect.tickle.domain.event.service.rabbitmq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplyResponseDto {
    private boolean success;
    private boolean winner;
    private String message;

    private ApplyResponseDto(boolean success, boolean winner, String message) {
        this.success = success;
        this.winner = winner;
        this.message = message;
    }

    public static ApplyResponseDto create(boolean success, boolean winner, String message) {
        return new ApplyResponseDto(success, winner, message);
    }

    public static ApplyResponseDto success(boolean winner) {
        return create(true, winner, "응모 성공");
    }

    public static ApplyResponseDto fail(String message) {
        return create(false, false, message);
    }

    public static ApplyResponseDto queued() {
        return create(true, false, "응모 요청이 접수되었습니다.");
    }
}