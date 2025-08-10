package com.profect.tickle.domain.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record SeatProjection(
        Long eventId,
        Long performanceId,
        String eventName,
        String seatNumber,
        Instant startDate,
        Instant endDate
) {}