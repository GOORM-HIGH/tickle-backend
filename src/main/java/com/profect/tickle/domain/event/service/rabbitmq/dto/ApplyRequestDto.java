package com.profect.tickle.domain.event.service.rabbitmq.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplyRequestDto {
    private Long eventId;
    private Long memberId;

    private ApplyRequestDto(Long eventId, Long memberId) {
        this.eventId = eventId;
        this.memberId = memberId;
    }

    public static ApplyRequestDto create(Long eventId, Long memberId) {
        return new ApplyRequestDto(eventId, memberId);
    }
}