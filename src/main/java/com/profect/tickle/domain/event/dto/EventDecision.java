package com.profect.tickle.domain.event.dto;

public record EventDecision (
        Long eventId,
        Long memberId,
        short perPrice,
        boolean winner,
        int accrued,
        Long seatId
){}