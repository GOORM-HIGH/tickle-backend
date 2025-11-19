package com.profect.tickle.domain.event.service.rabbitmq.dto;

public record PostReservationMessage(
        Long memberId,
        Long seatId,
        int accrued
) {}