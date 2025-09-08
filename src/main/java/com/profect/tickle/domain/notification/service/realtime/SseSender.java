package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.util.JsonUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    // properties
    private static final int MAX_REPLAY_PER_MEMBER = 50;
    private static final long REPLAY_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    // utils
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final Executor sseExecutor;

    private final ConcurrentMap<String, SerialExecutor> lanes = new ConcurrentHashMap<>();
    private final AtomicLong lastEventId = new AtomicLong(0);

    // repositories / properties
    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;
    private final MeterRegistry meterRegistry;

    // 메트릭 필드들 (초기화는 @PostConstruct에서)
    private Counter connectionsCreated;
    private Counter connectionsCompleted;
    private Counter connectionsTimeout;
    private Counter connectionsError;
    private Counter messagesSent;
    private Counter messagesFailed;

    @PostConstruct
    private void initMetrics() {
        // Counter 메트릭 초기화
        this.connectionsCreated = Counter.builder("sse_connections_created_total")
                .description("Total number of SSE connections created")
                .register(meterRegistry);

        this.connectionsCompleted = Counter.builder("sse_connections_completed_total")
                .description("Total number of SSE connections completed normally")
                .register(meterRegistry);

        this.connectionsTimeout = Counter.builder("sse_connections_timeout_total")
                .description("Total number of SSE connections timed out")
                .register(meterRegistry);

        this.connectionsError = Counter.builder("sse_connections_error_total")
                .description("Total number of SSE connections ended with error")
                .register(meterRegistry);

        this.messagesSent = Counter.builder("sse_messages_sent_total")
                .description("Total number of SSE messages sent successfully")
                .register(meterRegistry);

        this.messagesFailed = Counter.builder("sse_messages_failed_total")
                .description("Total number of SSE messages failed to send")
                .register(meterRegistry);

        Gauge.builder("sse_connections_total", () -> {
                    return sseRepository.getAllWithIdsGroupedByMember()
                            .values()
                            .stream()
                            .mapToLong(Map::size)
                            .sum();
                })
                .description("Current number of active SSE connections")
                .register(meterRegistry);

        Gauge.builder("sse_connections_members", () -> {
                    return sseRepository.getAllWithIdsGroupedByMember().size();
                })
                .description("Current number of active members with SSE connections")
                .register(meterRegistry);

        Gauge.builder("sse_connections_avg_per_member", () -> {
                    Map<Long, Map<String, SseEmitter>> grouped = sseRepository.getAllWithIdsGroupedByMember();
                    long totalConnections = grouped.values().stream().mapToLong(Map::size).sum();
                    long activeMembers = grouped.size();
                    return activeMembers > 0 ? (double) totalConnections / activeMembers : 0.0;
                })
                .description("Average SSE connections per member")
                .register(meterRegistry);
    }

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventIdHeader) {
        // 연결마다 고유 emitterId 생성
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();
        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid;

        log.info("SSE connect - memberId={}, emitterId={}", memberId, emitterId);

        // 🆕 연결 생성 메트릭 증가
        connectionsCreated.increment();

        // 타임아웃 설정
        SseEmitter emitter = new SseEmitter(notificationProperty.sseTimeout().toMillis());
        sseRepository.save(memberId, emitterId, emitter);
        setEmitter(memberId, emitter, emitterId);

        // 초기 핑(Last-Event-ID 체인 시작)
        try {
            long eventId = nextEventId();
            emitter.send(SseEmitter.event()
                    .name("sse-connect")
                    .id(Long.toString(eventId))
                    .data("connection was completed", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("initial send failed - {}, {}", emitterId, e.getMessage());
            disconnectEmitterWithError(memberId, emitterId, e);
            return emitter;
        }

        // 유실 이벤트 복원: 같은 emitter lane에 넣어 순서 보장
        if (lastEventIdHeader != null && !lastEventIdHeader.isBlank()) {
            laneOf(emitterId).execute(() -> resend(memberId, emitterId, emitter, lastEventIdHeader));
        }
        return emitter;
    }

    @Override
    public void send(long memberId, NotificationEnvelope<?> payload) {
        // 1) 이벤트 생성 + 직렬화 (항상 수행)
        long eventId = nextEventId();
        String json;

        try {
            json = JsonUtils.toJson(objectMapper, payload);
        } catch (Exception e) {
            log.warn("[SSE 전송] payload 직렬화에 실패했습니다. {}번 회원에게 전송하는 실시간알림 전송을 종료합니다.", memberId, e);
            messagesFailed.increment();
            throw new BusinessException(ErrorCode.REALTIME_NOTIFICATION_SEND_FAILED);
        }

        // 2) 유실 캐시 저장 + TTL 정리 (항상 수행)
        sseRepository.saveEvent(memberId, eventId, json);
        sseRepository.trimEvents(memberId, MAX_REPLAY_PER_MEMBER, eventId - REPLAY_TTL_MS);

        // 3) 활성 emitter 스냅샷 조회
        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            log.debug("no active SSE emitters; cached event for replay. memberId={}, eventId={}", memberId, eventId);
            return;
        }

        // 4) 전송 (같은 emitter 내에서는 직렬화된 순서 유지)
        targets.forEach((emitterId, emitter) -> {
            laneOf(emitterId).execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                    messagesSent.increment();
                } catch (IOException ex) {
                    log.warn("send failed - memberId={}, emitterId={}, err={}", memberId, emitterId, ex.toString());
                    messagesFailed.increment();
                    disconnectEmitterWithError(memberId, emitterId, ex);
                    removeLane(emitterId);
                }
            });
        });
    }

    @Override
    public void sendAll(NotificationEnvelope<?> payload) {
        // 브로드캐스트는 per-user 캐시를 만들지 않고, 현재 연결된 emitter에만 발송
        long eventId = nextEventId();
        String json = JsonUtils.toJson(objectMapper, payload);

        Map<Long, Map<String, SseEmitter>> snapshot = sseRepository.getAllWithIdsGroupedByMember();
        if (snapshot.isEmpty()) {
            log.debug("sendAll: no active SSE emitters; nothing to deliver.");
            return;
        }

        snapshot.forEach((memberId, emitters) -> {
            emitters.forEach((emitterId, emitter) -> {
                laneOf(emitterId).execute(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("notification")
                                .id(Long.toString(eventId))
                                .data(json, MediaType.APPLICATION_JSON));
                        // 브로드캐스트 메시지 전송 성공
                        messagesSent.increment();
                    } catch (IOException ex) {
                        log.warn("sendAll failed - memberId={}, emitterId={}, err={}",
                                memberId, emitterId, ex.toString());
                        // 브로드캐스트 메시지 전송 실패
                        messagesFailed.increment();
                        disconnectEmitterWithError(memberId, emitterId, ex);
                        removeLane(emitterId);
                    }
                });
            });
        });
    }

    private void setEmitter(long memberId, SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("onCompletion - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
            connectionsCompleted.increment();
        });

        emitter.onTimeout(() -> {
            log.warn("onTimeout - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
            connectionsTimeout.increment();
        });

        emitter.onError(e -> {
            log.warn("onError - {}: {}", emitterId, e.toString());
            disconnectEmitterWithError(memberId, emitterId, e);
            connectionsError.increment();
        });
    }

    @Override
    public void resend(long memberId, @Nullable String emitterId, SseEmitter emitter, String lastEventIdHeader) {
        final long last;
        try {
            last = Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("Invalid Last-Event-ID: {}", lastEventIdHeader);
            return;
        }

        NavigableMap<Long, String> later = sseRepository.eventsAfter(memberId, last);
        if (later.isEmpty()) {
            if (emitterId != null) {
                log.debug("replay skipped (no later events) - memberId={}, emitterId={}, lastEventId={}", memberId, emitterId, last);
            }
            return;
        }

        long latestId = later.lastKey();
        int missed = later.size();
        String payload = later.get(latestId);

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .id(Long.toString(latestId))
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
            // 재전송 중 끊기면 콜백에서 처리
        }

        if (emitterId != null) {
            log.debug("replay summarized - memberId={}, emitterId={}, lastEventId={}, latestId={}, missed={}",
                    memberId, emitterId, last, latestId, missed);
        }
    }

    @Override
    public void disconnectAll(long memberId) {
        Map<String, SseEmitter> targets = Map.copyOf(sseRepository.getAllWithIds(memberId));
        if (targets.isEmpty()) {
            log.debug("disconnectAll: no emitters for memberId={}", memberId);
            return;
        }

        targets.forEach((emitterId, e) -> {
            try {
                try {
                    e.send(SseEmitter.event().name("bye").data("closing"));
                } catch (IOException ignored) {
                }
                e.complete();
            } catch (Exception ex) {
                log.debug("disconnectAll: complete failed (memberId={}, emitterId={}) - {}", memberId, emitterId, ex.toString());
            } finally {
                removeLane(emitterId);
            }
        });

        sseRepository.removeAll(memberId);
        log.info("SSE disconnected all emitters - memberId={}, count={}", memberId, targets.size());
    }

    @Override
    public void disconnectEmitter(long memberId, String emitterId) {
        SseEmitter e = sseRepository.getByEmitterId(emitterId);
        if (e == null) {
            log.warn("disconnectEmitter: not found - memberId={}, emitterId={}", memberId, emitterId);
            return;
        }
        try {
            try {
                e.send(SseEmitter.event().name("bye").data("closing"));
            } catch (IOException ignored) {
            }
            e.complete();
        } catch (Exception ignored) {
        } finally {
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
        }
    }

    @Override
    public void disconnectEmitterWithError(long memberId, String emitterId, Throwable cause) {
        SseEmitter e = sseRepository.getByEmitterId(emitterId);
        if (e == null) {
            log.warn("disconnectEmitterWithError: not found - memberId={}, emitterId={}", memberId, emitterId);
            return;
        }
        try {
            e.completeWithError(cause);
        } catch (Exception ignored) {
        } finally {
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
        }
    }

    private SerialExecutor laneOf(String emitterId) {
        return lanes.computeIfAbsent(emitterId, id -> new SerialExecutor(sseExecutor));
    }

    private long nextEventId() {
        while (true) {
            long prev = lastEventId.get();
            long candidate = Math.max(prev + 1, clock.millis());
            if (lastEventId.compareAndSet(prev, candidate)) return candidate;
        }
    }

    private void removeLane(String emitterId) {
        lanes.remove(emitterId);
    }

    static final class SerialExecutor implements Executor {
        private final Executor backend;
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private Runnable active;

        SerialExecutor(Executor backend) {
            this.backend = backend;
        }

        @Override
        public synchronized void execute(Runnable r) {
            tasks.add(() -> {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            });
            if (active == null) scheduleNext();
        }

        private synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) backend.execute(active);
        }
    }
}
