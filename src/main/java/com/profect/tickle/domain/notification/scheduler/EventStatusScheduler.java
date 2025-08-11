package com.profect.tickle.domain.notification.scheduler;

import com.profect.tickle.domain.event.mapper.EventMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventMapper eventMapper;

    @Scheduled(cron = "0 * * * * *") // 매 분 0초
    @Transactional
    public void syncEventStatuses() {
        int toOngoing = eventMapper.markEventsAsOngoing();
        int toFinished = eventMapper.markEventsAsFinished();
        // 필요하면 로그
        // log.info("event status updated: toOngoing={}, toFinished={}", toOngoing, toFinished);
    }
}