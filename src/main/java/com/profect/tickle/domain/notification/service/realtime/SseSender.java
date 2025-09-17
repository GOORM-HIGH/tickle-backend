package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.util.constant.NotificationBatchConstants;
import com.profect.tickle.global.util.JsonUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final Executor sseExecutor;
    private final AtomicLong lastEventId = new AtomicLong(0);

    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;
    private final MeterRegistry meterRegistry;

    private Counter connectionsCreated;
    private Counter connectionsCompleted;
    private Counter connectionsTimeout;
    private Counter connectionsError;
    private Counter messagesSent;

    @PostConstruct
    private void init() {
        initMetrics();
    }

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventIdHeader) {
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();
        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid;

        log.info("SSE 연결 - 회원ID={}, 송신자ID={}", memberId, emitterId);
        connectionsCreated.increment();

        SseEmitter emitter = new SseEmitter(notificationProperty.sseTimeout().toMillis());
        setEmitter(memberId, emitter, emitterId);
        sseRepository.save(memberId, emitterId, emitter);

        try {
            long eventId = nextEventId();
            emitter.send(SseEmitter.event()
                    .name("sse-connect")
                    .id(Long.toString(eventId))
                    .data("connection was completed", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("초기 전송 실패 - {}, {}", emitterId, e.getMessage());
            disconnectEmitterWithError(memberId, emitterId, e);
            return emitter;
        }

        // 재연결 시 누락된 메시지는 클라이언트가 별도 API 호출로 처리
        if (lastEventIdHeader != null && !lastEventIdHeader.isBlank()) {
            log.debug("재연결 감지: 회원ID={}, Last-Event-ID={} - 클라이언트에서 별도 API로 누락 메시지 처리",
                    memberId, lastEventIdHeader);
        }

        return emitter;
    }

    // 개별 사용자에게 SSE 메시지 전송
    @Override
    public boolean send(long memberId, NotificationEnvelope<?> payload) {
        try {
            long eventId = nextEventId();
            String json = JsonUtils.toJson(objectMapper, payload);

            Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
            if (targets.isEmpty()) {
                log.debug("활성 SSE 송신자 없음: 회원ID={}", memberId);
                return true;
            }

            for (Map.Entry<String, SseEmitter> entry : targets.entrySet()) {
                String emitterId = entry.getKey();
                SseEmitter emitter = entry.getValue();

                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                    messagesSent.increment();
                    return true;
                } catch (IOException ex) {
                    log.debug("전송 실패: 회원ID={}, 송신자ID={}", memberId, emitterId);
                    // onError 콜백이 자동으로 연결 정리
                }
            }

            return false;

        } catch (Exception e) {
            log.error("개별 전송 중 예외: 회원ID={}", memberId, e);
            return false;
        }
    }

    // 모든 연결된 사용자에게 브로드캐스트 전송
    @Override
    public boolean sendAll(NotificationEnvelope<?> payload) {
        try {
            long startTime = clock.millis();
            long eventId = nextEventId();
            String json = JsonUtils.toJson(objectMapper, payload);

            Map<Long, Map<String, SseEmitter>> snapshot = sseRepository.getAllWithIdsGroupedByMember();
            if (snapshot.isEmpty()) {
                log.debug("브로드캐스트 건너뜀: 활성 연결 없음");
                return false;
            }

            log.info("브로드캐스트 시작: 회원수={}", snapshot.size());

            List<Long> memberIdList = new ArrayList<>(snapshot.keySet());
            List<List<Long>> memberChunkList = createChunkList(memberIdList);

            AtomicBoolean allSuccess = new AtomicBoolean(true);

            List<CompletableFuture<Void>> futureList = memberChunkList.stream()
                    .map(chunk -> CompletableFuture.runAsync(() -> {
                        sendAllToChunk(chunk, snapshot, eventId, json, allSuccess);
                    }, sseExecutor))
                    .toList();

            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

            long duration = clock.millis() - startTime;
            boolean finalResult = allSuccess.get();

            log.info("브로드캐스트 완료: 소요시간={}ms, 전체 성공 여부={}",
                    duration, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("브로드캐스트 중 예외", e);
            return false;
        }
    }

    @Override
    public void disconnectAll(long memberId) {
        Map<String, SseEmitter> targets = Map.copyOf(sseRepository.getAllWithIds(memberId));
        if (targets.isEmpty()) {
            log.debug("전체 연결해제: 회원ID={} 송신자 없음", memberId);
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
                log.debug("전체 연결해제: 완료 실패 (회원ID={}, 송신자ID={}) - {}",
                        memberId, emitterId, ex.toString());
            }
        });
        sseRepository.removeAll(memberId);
        log.info("SSE 모든 송신자 연결해제 - 회원ID={}, 개수={}", memberId, targets.size());
    }

    @Override
    public void disconnectEmitter(long memberId, String emitterId) {
        SseEmitter e = sseRepository.getByEmitterId(emitterId);
        if (e == null) {
            log.warn("송신자 연결해제: 찾을 수 없음 - 회원ID={}, 송신자ID={}", memberId, emitterId);
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

    public void disconnectEmitterWithError(long memberId, String emitterId, Throwable cause) {
        SseEmitter e = sseRepository.getByEmitterId(emitterId);
        if (e == null) {
            log.warn("오류로 송신자 연결해제: 찾을 수 없음 - 회원ID={}, 송신자ID={}", memberId, emitterId);
            return;
        }

        try {
            e.completeWithError(cause);
        } catch (Exception ignored) {
        } finally {
            sseRepository.remove(memberId, emitterId);
        }
    }

    // Emitter 설정 메서드
    private void setEmitter(long memberId, SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("연결 완료 - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            connectionsCompleted.increment();
        });

        emitter.onTimeout(() -> {
            log.warn("연결 시간초과 - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            connectionsTimeout.increment();
        });

        emitter.onError(e -> {
            log.warn("연결 오류 - {}: {}", emitterId, e.toString());
            disconnectEmitterWithError(memberId, emitterId, e);
            connectionsError.increment();
        });
    }

    private <T> List<List<T>> createChunkList(List<T> list) {
        List<List<T>> chunkList = new ArrayList<>();
        for (int i = 0; i < list.size(); i += NotificationBatchConstants.CHUNK_SIZE) {
            int end = Math.min(i + NotificationBatchConstants.CHUNK_SIZE, list.size());
            chunkList.add(list.subList(i, end));
        }
        return chunkList;
    }

    private void sendAllToChunk(List<Long> memberChunk,
                                Map<Long, Map<String, SseEmitter>> snapshot,
                                long eventId, String json,
                                AtomicBoolean allSuccess) {
        for (Long memberId : memberChunk) {
            Map<String, SseEmitter> emitters = snapshot.get(memberId);
            if (emitters == null || emitters.isEmpty()) {
                allSuccess.set(false);
                continue;
            }

            boolean memberSuccess = false;
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                SseEmitter emitter = entry.getValue();

                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                    messagesSent.increment();
                    memberSuccess = true;
                } catch (IOException ex) {
                    // onError 콜백이 자동 처리
                }
            }

            if (!memberSuccess) {
                allSuccess.set(false);
            }
        }
    }

    private long nextEventId() {
        while (true) {
            long prev = lastEventId.get();
            long candidate = Math.max(prev + 1, clock.millis());
            if (lastEventId.compareAndSet(prev, candidate)) return candidate;
        }
    }

    private void initMetrics() {
        this.connectionsCreated = Counter.builder("sse_connections_created_total")
                .description("생성된 SSE 연결 총 개수")
                .register(meterRegistry);

        this.connectionsCompleted = Counter.builder("sse_connections_completed_total")
                .description("정상적으로 완료된 SSE 연결 총 개수")
                .register(meterRegistry);

        this.connectionsTimeout = Counter.builder("sse_connections_timeout_total")
                .description("시간 초과된 SSE 연결 총 개수")
                .register(meterRegistry);

        this.connectionsError = Counter.builder("sse_connections_error_total")
                .description("오류로 종료된 SSE 연결 총 개수")
                .register(meterRegistry);

        this.messagesSent = Counter.builder("sse_messages_sent_total")
                .description("성공적으로 전송된 SSE 메시지 총 개수")
                .register(meterRegistry);

        Gauge.builder("sse_connections_total", () -> {
                    return sseRepository.getAllWithIdsGroupedByMember()
                            .values()
                            .stream()
                            .mapToLong(Map::size)
                            .sum();
                })
                .description("현재 활성 SSE 연결 수")
                .register(meterRegistry);

        Gauge.builder("sse_connections_members", () -> {
                    return sseRepository.getAllWithIdsGroupedByMember().size();
                })
                .description("SSE 연결을 가진 현재 활성 회원 수")
                .register(meterRegistry);

        Gauge.builder("sse_connections_avg_per_member", () -> {
                    Map<Long, Map<String, SseEmitter>> grouped = sseRepository.getAllWithIdsGroupedByMember();
                    long totalConnections = grouped.values().stream().mapToLong(Map::size).sum();
                    long activeMembers = grouped.size();
                    return activeMembers > 0 ? (double) totalConnections / activeMembers : 0.0;
                })
                .description("회원당 평균 SSE 연결 수")
                .register(meterRegistry);
    }

    @PreDestroy
    private void cleanup() {
        log.info("SSE 전송자 종료");
    }
}
