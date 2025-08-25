package com.profect.tickle.domain.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@TestConfiguration
public class SseNotificationTestConfig {

    // 테스트는 동기로 진행
    @Bean
    @Primary
    public Executor sseExecutor() {
        return Runnable::run;
    }

    @Bean
    public NotificationProperty notificationProperty() {
        NotificationProperty mock = Mockito.mock(NotificationProperty.class);
        Mockito.when(mock.sseTimeout()).thenReturn(Duration.ofMinutes(5));
        return mock;
    }

    @Bean
    @Primary
    public SseRepository sseRepository() {
        return new InMemorySseRepository();
    }

    // 테스트용 인메모리 SseRepository
    static class InMemorySseRepository extends SseRepository {
        private final ConcurrentMap<Long, ConcurrentMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();
        private final ConcurrentMap<Long, ConcurrentSkipListMap<Long, String>> events = new ConcurrentHashMap<>();

        @Override
        public void save(long memberId, String emitterId, SseEmitter emitter) {
            emitters.computeIfAbsent(memberId, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        }

        @Override
        public Map<String, SseEmitter> getAllWithIds(long memberId) {
            return new HashMap<>(emitters.getOrDefault(memberId, new ConcurrentHashMap<>()));
        }

        @Override
        public Collection<SseEmitter> getAll(long memberId) {
            return getAllWithIds(memberId).values();
        }

        @Override
        public SseEmitter getByEmitterId(String emitterId) {
            return emitters.values().stream().map(m -> m.get(emitterId))
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }

        @Override
        public void remove(long memberId, String emitterId) {
            var m = emitters.get(memberId);
            if (m != null) m.remove(emitterId);
        }

        @Override
        public void removeAll(long memberId) {
            emitters.remove(memberId);
        }

        @Override
        public void saveEvent(long memberId, long eventId, String json) {
            events.computeIfAbsent(memberId, k -> new ConcurrentSkipListMap<>()).put(eventId, json);
        }

        @Override
        public NavigableMap<Long, String> eventsAfter(long memberId, long lastEventId) {
            return new TreeMap<>(events.getOrDefault(memberId, new ConcurrentSkipListMap<>())
                    .tailMap(lastEventId, false));
        }

        @Override
        public void clearEventsBefore(long memberId, long thresholdEventId) {
            var m = events.get(memberId);
            if (m != null) m.headMap(thresholdEventId, false).clear();
        }
    }
}
