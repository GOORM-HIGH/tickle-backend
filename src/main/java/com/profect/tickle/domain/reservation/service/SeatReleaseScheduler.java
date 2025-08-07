package com.profect.tickle.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatReleaseScheduler {

    private final SeatPreemptionReleaser releaser;

    @Scheduled(cron = "*/10 * * * * *")
    public void releaseExpiredSeats() {
        releaser.releaseExpiredPreemptions();
    }
}
