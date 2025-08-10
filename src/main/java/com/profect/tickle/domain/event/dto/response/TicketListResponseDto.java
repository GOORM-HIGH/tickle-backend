package com.profect.tickle.domain.event.dto.response;

import java.time.Instant;

public record TicketListResponseDto(
        Long eventId,
        String name,
        Short perPrice,
        String img,
        Long statusId,
        Instant startDate,
        Instant endDate
) implements EventListResponseDto {

    @Override
    public Long getEventId() {
        return eventId;
    }

    @Override
    public String getName() {
        return name;
    }
}