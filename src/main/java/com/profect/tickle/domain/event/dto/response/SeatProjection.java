package com.profect.tickle.domain.event.dto.response;

public record SeatProjection(
        Long eventId,
        Long performanceId,
        String eventName,
        String seatNumber
) {}