package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.global.util.JsonUtils;
import jakarta.annotation.Nullable;
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
    private static final int MAX_REPLAY_PER_MEMBER = 50;                                // 개수 상한
    private static final long REPLAY_TTL_MS = TimeUnit.MINUTES.toMillis(10);    // TTL

    // utils
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final Executor sseExecutor;

    private final ConcurrentMap<String, SerialExecutor> lanes = new ConcurrentHashMap<>();    // 순서 저장용 맵
    private final AtomicLong lastEventId = new AtomicLong(0);                       // SSE 아이디 카운터

    // repositories / properties
    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventIdHeader) {
        // 연결마다 고유 emitterId 생성
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();
        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid;

        log.info("SSE connect - memberId={}, emitterId={}", memberId, emitterId);

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
                    .data("connected"));
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
        long eventId = nextEventId();
        String json = JsonUtils.toJson(objectMapper, payload);

        // 1) 활성 emitter 조회 (스냅샷)
        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            // 오프라인이면 캐시 저장 생략 (클라가 API Pull로 동기화)
            log.debug("no active SSE emitters; skip caching. memberId={}, eventId={}", memberId, eventId);
            return;
        }

        // 2) 유실 이벤트 캐시 저장 + 트리밍
        sseRepository.saveEvent(memberId, eventId, json);
        sseRepository.trimEvents(memberId, MAX_REPLAY_PER_MEMBER, eventId - REPLAY_TTL_MS);

        // 3) emitter별 전송, 같은 emitter 내에서는 lane으로 직렬화
        targets.forEach((emitterId, emitter) -> {
            laneOf(emitterId).execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    log.warn("send failed - memberId={}, emitterId={}, err={}", memberId, emitterId, ex.toString());
                    disconnectEmitterWithError(memberId, emitterId, ex);
                    removeLane(emitterId); // lane 정리
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
                    } catch (IOException ex) {
                        log.warn("sendAll failed - memberId={}, emitterId={}, err={}",
                                memberId, emitterId, ex.toString());
                        disconnectEmitterWithError(memberId, emitterId, ex);
                        removeLane(emitterId); // lane 정리
                    }
                });
            });
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

        // 1) 최신 이벤트 ID 하나만 사용 (프론트엔드가 API를 호출하도록 하는 '신호')
        long latestId = later.lastKey();
        int missed = later.size();
        String payload = later.get(latestId);

        // 2) 전송
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
        // 스냅샷을 떠서 안전하게 순회 (id 포함)
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

    private void setEmitter(long memberId, SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("onCompletion - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
        });
        emitter.onTimeout(() -> {
            log.warn("onTimeout - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
        });
        emitter.onError(e -> {
            log.warn("onError - {}: {}", emitterId, e.toString());
            disconnectEmitterWithError(memberId, emitterId, e);
        });
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

    // ---- 간단한 직렬 실행기 (Guava SerializingExecutor 유사)
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
