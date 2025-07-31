package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;

public record TicketListResponseDto(
        Long id,
        String name,
        Integer goalPrice,
        Short perPrice
) implements EventListResponseDto {
    public static TicketListResponseDto from(Event event) {
        return new TicketListResponseDto(
                event.getId(),
                event.getName(),
                event.getGoalPrice(),
                event.getPerPrice()
        );
    }

    @Override
    public Long getEventId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}