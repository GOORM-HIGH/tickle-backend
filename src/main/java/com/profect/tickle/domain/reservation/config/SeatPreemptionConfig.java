package com.profect.tickle.domain.reservation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seat.preemption")
@Data
public class SeatPreemptionConfig {
    private int maxSeatsPerMember = 5;
    private int preemptionDurationMinutes = 5;
}
