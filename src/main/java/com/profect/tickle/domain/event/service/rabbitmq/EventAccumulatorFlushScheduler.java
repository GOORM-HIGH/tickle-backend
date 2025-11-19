package com.profect.tickle.domain.event.service.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventAccumulatorFlushScheduler {

    private final EventAccumulatorFlushService flushService;

    @Scheduled(fixedDelay = 30_000)
    public void scheduleFlush() {
        flushService.flushToDB();
    }
}