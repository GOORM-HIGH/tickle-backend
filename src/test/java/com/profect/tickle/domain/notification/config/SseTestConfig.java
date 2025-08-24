package com.profect.tickle.domain.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
import java.util.function.Supplier;

@TestConfiguration
public class SseTestConfig {

    @Bean
    Clock clock() {
        return Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    Executor sseExecutor() {
        return Runnable::run; // 테스트는 동기로 결정적 실행
    }

    @Bean
    NotificationProperty notificationProperty() {
        NotificationProperty mock = org.mockito.Mockito.mock(NotificationProperty.class);
        org.mockito.Mockito.when(mock.sseTimeout()).thenReturn(Duration.ofMinutes(5));
        return mock;
    }

    @Bean
    SseRepository sseRepository() {
        return new InMemorySseRepository();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // 호출마다 다른 UUID를 돌려주는 Supplier (emitterId 충돌 방지)
    @Bean
    Supplier<UUID> uuidSupplier() {
        AtomicLong seq = new AtomicLong(1);
        return () -> new UUID(0L, seq.getAndIncrement());
    }

    // 테스트용 인메모리 Repo≠
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
