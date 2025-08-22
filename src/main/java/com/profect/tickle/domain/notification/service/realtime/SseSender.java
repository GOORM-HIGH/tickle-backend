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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.Executor;
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

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventId) {
        // 연결마다 고유 emitterId 생성
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();
        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid;

        log.info("📡 SSE connect - memberId={}, emitterId={}", memberId, emitterId);

        // 타임아웃 설정
        SseEmitter emitter = new SseEmitter(notificationProperty.sseTimeout().toMillis());
        sseRepository.save(memberId, emitterId, emitter);
        setEmitter(memberId, emitter, emitterId);

        // 초기 핑(Last-Event-ID 체인 시작)
        try {
            long eventId = clock.millis();
            emitter.send(SseEmitter.event()
                    .name("sse-connect")
                    .id(Long.toString(eventId))
                    .data("connected"));
        } catch (IOException e) {
            log.error("❌ initial send failed - {}, {}", emitterId, e.getMessage());
            sseRepository.remove(memberId, emitterId);
            emitter.completeWithError(e);
            return emitter;
        }

        // 유실 이벤트 복원은 전용 풀로 비동기 처리
        if (lastEventId != null && !lastEventId.isBlank()) {
            sseExecutor.execute(() -> resend(memberId, emitter, lastEventId));
        }
        return emitter;
    }

    @Override
    public void send(long memberId, NotificationEnvelope<?> payload) {
        // 0) 이벤트 ID/페이로드 준비
        long eventId = clock.millis(); // 단조 증가 가정(밀리초)
        String json = NotificationUtil.toJson(objectMapper, payload);

        // 1) 유저별 이벤트 캐시 저장(오프라인일 때 재전송용)
        sseRepository.saveEvent(memberId, eventId, json);
        // 필요 시 오래된 캐시 정리 (예시)
        // sseRepository.clearEventsBefore(memberId, eventId - TimeUnit.MINUTES.toMillis(10));

        // 2) 활성 emitter 조회
        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            log.debug("ℹ️ no active SSE emitters; cached event for replay. memberId={}, eventId={}", memberId, eventId);
            return;
        }

        // 3) 비동기 전송
        sseExecutor.execute(() -> {
            targets.forEach((emitterId, emitter) -> {
                try {
                    // emitter 단위 직렬화를 위한 최소 동기화(필요 시 SerialExecutor로 대체)
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event()
                                .name("notification")
                                .id(Long.toString(eventId))
                                .data(json, MediaType.APPLICATION_JSON));
                    }
                } catch (IOException ex) {
                    log.warn("⚠️ send failed - memberId={}, emitterId={}, err={}", memberId, emitterId, ex.toString());
                    try {
                        emitter.completeWithError(ex);
                    } catch (Exception ignore) {
                    }
                    sseRepository.remove(memberId, emitterId);
                }
            });
        });
    }

    @Override
    public void resend(long memberId, SseEmitter emitter, String lastEventId) {
        final long last;
        try {
            last = Long.parseLong(lastEventId);
        } catch (NumberFormatException ex) {
            log.warn("Invalid Last-Event-ID: {}", lastEventId);
            return;
        }

        // last 이후의 이벤트만 정렬된 상태로 가져오기
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
    }

    @Override
    public void disconnectAll(long memberId) {
        // 스냅샷을 떠서 안전하게 순회
        var emitters = List.copyOf(sseRepository.getAll(memberId));
        if (emitters.isEmpty()) {
            log.debug("disconnectAll: no emitters for memberId={}", memberId);
            return;
        }

        for (SseEmitter e : emitters) {
            try {
                try {
                    e.send(SseEmitter.event().name("bye").data("closing"));
                } catch (IOException ignored) {
                }
                e.complete();
            } catch (Exception ex) {
                log.debug("disconnectAll: complete failed (memberId={}) - {}", memberId, ex.toString());
            }
        }

        sseRepository.removeAll(memberId);
        log.info("SSE disconnected all emitters - memberId={}, count={}", memberId, emitters.size());
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
        }
    }

    private void setEmitter(long memberId, SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("🧹 onCompletion - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
        });
        emitter.onTimeout(() -> {
            log.warn("⏱️ onTimeout - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
        });
        emitter.onError(e -> {
            log.warn("⚠️ onError - {}: {}", emitterId, e.toString());
            sseRepository.remove(memberId, emitterId);
        });
    }
}
