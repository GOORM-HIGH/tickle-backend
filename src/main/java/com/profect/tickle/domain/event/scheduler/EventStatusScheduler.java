package com.profect.tickle.domain.event.scheduler;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.mapper.EventMapper;
import com.profect.tickle.global.redis.service.RedisInitialize;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventMapper eventMapper;
    private final RedisInitialize redisInitialize;

    @Scheduled(cron = "0 * * * * *") //매일 자정
    @Transactional
    public void syncEventStatuses() {
        List<Event> eventsToStart = eventMapper.findEventsToMarkAsOngoing();
        int toOngoing = eventMapper.markEventsAsOngoing();
        int toFinished = eventMapper.markEventsAsFinished();

        for (Event event : eventsToStart) {
            redisInitialize.initializeRedisEvent(event); // Redis 초기화
        }
    }
}