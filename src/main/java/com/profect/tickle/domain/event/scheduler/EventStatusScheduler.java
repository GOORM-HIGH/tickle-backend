package com.profect.tickle.domain.event.scheduler;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.mapper.EventMapper;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.global.redis.service.RedisInitialize;
import com.profect.tickle.global.status.StatusIds;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventRepository eventRepository;
    private final RedisInitialize redisInitialize;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void syncEventStatuses() {
        List<Event> eventsToStart = eventRepository.findEventsToStart(StatusIds.Event.SCHEDULED);

        log.info("toOngoing={}개 이벤트 Redis 초기화 시작", eventsToStart.size());

        for (Event event : eventsToStart) {
            if (event == null) continue;
            log.info("Redis 초기화 대상 eventId={}, goalPrice={}", event.getId(), event.getGoalPrice());
            redisInitialize.initializeRedisEvent(event);
        }
    }
}