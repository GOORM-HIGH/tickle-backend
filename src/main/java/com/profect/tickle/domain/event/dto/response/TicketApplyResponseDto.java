package com.profect.tickle.domain.event.dto.response;

public record TicketApplyResponseDto(
        Long eventId,
        Long memberId,
        boolean message
) {
}