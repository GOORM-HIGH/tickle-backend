package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.util.NotificationUtil;
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

    // utils
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final Executor sseExecutor;

    // repositories / properties
    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;

    // ---- per-emitter 직렬 실행기(동일 emitter는 순서 보장, emitter 간 병렬 허용)
    private final ConcurrentMap<String, SerialExecutor> lanes = new ConcurrentHashMap<>();

    private SerialExecutor laneOf(String emitterId) {
        return lanes.computeIfAbsent(emitterId, id -> new SerialExecutor(sseExecutor));
    }

    private void removeLane(String emitterId) {
        lanes.remove(emitterId);
    }

    // ---- 단조 증가 event id (system clock 역행에도 보정)
    private final AtomicLong lastEventId = new AtomicLong(0);

    private long nextEventId() {
        while (true) {
            long prev = lastEventId.get();
            long candidate = Math.max(prev + 1, clock.millis());
            if (lastEventId.compareAndSet(prev, candidate)) return candidate;
        }
    }

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
            laneOf(emitterId).execute(() -> resendInternal(memberId, emitterId, emitter, lastEventIdHeader));
        }
        return emitter;
    }

    @Override
    public void send(long memberId, NotificationEnvelope<?> payload) {
        // 0) 이벤트 ID/페이로드 준비
        long eventId = nextEventId();
        String json = NotificationUtil.toJson(objectMapper, payload);

        // 1) 유저별 이벤트 캐시 저장(오프라인일 때 재전송용)
        sseRepository.saveEvent(memberId, eventId, json);
        // 오래된 캐시 정리(예: 10분)
        sseRepository.clearEventsBefore(memberId, eventId - TimeUnit.MINUTES.toMillis(10));

        // 2) 활성 emitter 조회 (스냅샷)
        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            log.debug("no active SSE emitters; cached event for replay. memberId={}, eventId={}", memberId, eventId);
            return;
        }

        // 3) emitter별로 병렬 전송, 단 같은 emitter 내에서는 lane으로 직렬화
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
                    removeLane(emitterId); // 🧹 lane 정리
                }
            });
        });
    }

    @Override
    public void resend(long memberId, SseEmitter emitter, String lastEventIdHeader) {
        // 인터페이스 호환용(외부에서 직접 호출될 수 있음) - emitterId를 알 수 없으므로 즉시 수행
        // connect() 경로에서는 resendInternal(memberId, emitterId, ...)로 lane을 통해 수행됨
        resendInternal(memberId, /*emitterId*/ null, emitter, lastEventIdHeader);
    }

    // lane을 사용할 수 있도록 emitterId를 받는 내부 구현
    private void resendInternal(long memberId, @Nullable String emitterId, SseEmitter emitter, String lastEventIdHeader) {
        final long last;
        try {
            last = Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("Invalid Last-Event-ID: {}", lastEventIdHeader);
            return;
        }

        NavigableMap<Long, String> later = sseRepository.eventsAfter(memberId, last);
        for (Map.Entry<Long, String> entry : later.entrySet()) {
            long eid = entry.getKey();
            String data = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(Long.toString(eid))
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException ignored) {
                // 재전송 중 끊기면 콜백(onError/onTimeout)에서 정리됨
                break;
            }
        }

        if (emitterId != null) {
            log.debug("replay completed - memberId={}, emitterId={}, lastEventId={}", memberId, emitterId, last);
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
                removeLane(emitterId); // 🧹 lane 정리
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
            removeLane(emitterId); // 🧹 lane 정리
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
            removeLane(emitterId); // 🧹 lane 정리
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
            // disconnectEmitterWithError 내에서 lane 제거
        });
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
