package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.util.NotificationUtil;
import com.profect.tickle.domain.notification.util.RealtimeEventUtil;
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
import java.util.*;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    // utils
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    // repositories
    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventId) {
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();

        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid; // 연결마다 사용할 고유 ID
        log.info("📡 SSE connect - memberId={}, emitterId={}", memberId, emitterId);

        SseEmitter emitter = new SseEmitter(notificationProperty.sseTimeout().toMillis());
        sseRepository.save(memberId, emitterId, emitter);

        setEmitter(memberId, emitter, emitterId);

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

        if (lastEventId != null && !lastEventId.isBlank()) {
            resend(memberId, emitter, lastEventId);
        }
        return emitter;
    }

    @Override
    public void send(long memberId, NotificationEnvelope<?> payload) {
        // 0) 이벤트 ID/페이로드 준비
        long eventId = clock.millis();
        String json = NotificationUtil.toJson(objectMapper, payload);

        // 1) 유저별 이벤트 캐시 저장 (오프라인이어도 복원 가능하도록)
        sseRepository.saveEvent(memberId, eventId, json);

        // 2) 활성 emitter 수신자 조회 (id 포함)
        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            log.debug("ℹ️ no active SSE emitters; cached event for later replay. memberId={}, eventId={}", memberId, eventId);
            return;
        }

        // 3) 전송
        for (Map.Entry<String, SseEmitter> entry : targets.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter e = entry.getValue();
            try {
                e.send(SseEmitter.event()
                        .name("notification")
                        .id(Long.toString(eventId))
                        .data(json, MediaType.APPLICATION_JSON));
            } catch (IOException ex) {
                // 끊긴 연결: 레지스트리 정리 + 종료 시그널 시도
                log.warn("⚠️ send failed - memberId={}, emitterId={}, err={}", memberId, emitterId, ex.toString());
                try {
                    e.completeWithError(ex);
                } catch (Exception ignore) {
                }
                sseRepository.remove(memberId, emitterId);
            }
        }
    }

    @Override
    public void resend(long memberId, SseEmitter emitter, String lastEventId) {
        long last;
        try {
            last = Long.parseLong(lastEventId);
        } catch (NumberFormatException ex) {
            log.warn("Invalid Last-Event-ID: {}", lastEventId);
            return;
        }

        NavigableMap<Long, String> later = sseRepository.eventsAfter(memberId, last);
        for (Map.Entry<Long, String> entry : later.entrySet()) {
            long eid = entry.getKey();
            String data = entry.getValue();
            if (!RealtimeEventUtil.isAfterEventId(String.valueOf(eid), lastEventId)) continue;
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(String.valueOf(eid))
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException ignored) {
                // 재전송 중 끊기면 onError/onTimeout에서 정리됨
                break;
            }
        }
    }

    @Override
    public void disconnectAll(long memberId) {
        // 1) 스냅샷을 떠서 안전하게 순회
        var emitters = List.copyOf(sseRepository.getAll(memberId));
        if (emitters.isEmpty()) {
            log.debug("disconnectAll: no emitters for memberId={}", memberId);
            return;
        }

        // 2) 각각 종료 시도
        for (SseEmitter e : emitters) {
            try {
                // 종료 알림 보내기 (실패하더라도 무시)
                try {
                    e.send(SseEmitter.event().name("bye").data("closing"));
                } catch (IOException ignored) {
                }

                e.complete();  // 정상 종료
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
