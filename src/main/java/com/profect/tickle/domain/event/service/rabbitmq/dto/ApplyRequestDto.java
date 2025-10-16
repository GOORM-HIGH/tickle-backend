package com.profect.tickle.domain.event.service.rabbitmq.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplyRequestDto {
    private Long eventId;
    private Long memberId;
    private Integer goalPrice;
    private Integer accrued;
    private Short perPrice;

    private ApplyRequestDto(Long eventId, Long memberId, Integer goalPrice, Integer accrued, Short perPrice) {
        this.eventId = eventId;
        this.memberId = memberId;
        this.goalPrice = goalPrice;
        this.accrued = accrued;
        this.perPrice = perPrice;
    }

    public static ApplyRequestDto create(Long eventId, Long memberId, Integer goalPrice, Integer accrued, Short perPrice) {
        return new ApplyRequestDto(eventId, memberId, goalPrice, accrued, perPrice);
    }
}