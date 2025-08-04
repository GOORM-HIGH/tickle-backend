package com.profect.tickle.domain.event.dto.response;

public record TicketListResponseDto(
        Long id,
        String name,
        Short perPrice,
        String img
) implements EventListResponseDto {

    @Override
    public Long getEventId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}