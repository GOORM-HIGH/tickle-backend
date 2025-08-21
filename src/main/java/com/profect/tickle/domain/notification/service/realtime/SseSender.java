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
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    // uitls
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
        Collection<SseEmitter> emitters = sseRepository.getAll(memberId);
        if (emitters.isEmpty()) {
            log.debug("ℹ️ no active SSE emitters for memberId={}", memberId);
            return;
        }

        long eventId = System.currentTimeMillis();
        String json = NotificationUtil.toJson(objectMapper, payload);

        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event()
                        .name("notification")
                        .id(String.valueOf(eventId))
                        .data(json, MediaType.APPLICATION_JSON));
            } catch (IOException ex) {
                // 개별 emitter가 끊겼다면 해당 emitter만 정리
                log.warn("⚠️ send failed to one emitter (memberId={}) - {}", memberId, ex.toString());
                // emitterId를 갖고 있지 않으니, Repository에 "끊긴 emitter를 제거" API가 있다면 호출하세요.
                // (예: sseRepository.removeByEmitterInstance(memberId, e))
            }
        }
        // 유저별 이벤트 캐시에 저장 (유실 복원용)
        sseRepository.saveEvent(memberId, eventId, json);
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
        Collection<SseEmitter> emitters = sseRepository.getAll(memberId);
        for (SseEmitter e : emitters) {
            try {
                try {
                    e.send(SseEmitter.event().name("bye").data("closing"));
                } catch (IOException ignored) {
                }
                e.complete();
            } catch (Exception ignored) {
            }
        }
        // 모든 emitterId를 내부에서 제거하도록 Repository 쪽에서 처리(또는 별도 메서드 제공)
        // 예: sseRepository.removeAll(memberId);
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
