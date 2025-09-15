package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.util.JsonUtils;
import com.profect.tickle.global.util.SerialExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    private static final int CHUNK_SIZE = 500;
    private static final int MAX_REPLAY_PER_MEMBER = 50;
    private static final long REPLAY_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    private final Executor sseExecutor;
    private final ConcurrentMap<String, SerialExecutor> lanes = new ConcurrentHashMap<>();
    private final AtomicLong lastEventId = new AtomicLong(0);

    private final NotificationProperty notificationProperty;
    private final SseRepository sseRepository;
    private final MeterRegistry meterRegistry;

    private final StreamOperations<String, String, Object> streamOperations;

    @Value("#{@notificationStreamKey}")
    private String notificationStreamKey;

    private static final String CONSUMER_GROUP = "sse-notification-processors";
    private static final String CONSUMER_NAME = "sse-sender";

    private Counter connectionsCreated;
    private Counter connectionsCompleted;
    private Counter connectionsTimeout;
    private Counter connectionsError;
    private Counter messagesSent;
    private Counter messagesFailed;

    @PostConstruct
    private void init() {
        initMetrics();
        initStreamConsumer();
    }

    private void initStreamConsumer() {
        try {
            try {
                streamOperations.createGroup(notificationStreamKey, CONSUMER_GROUP);
                log.info("소비자 그룹 생성: stream={}, group={}", notificationStreamKey, CONSUMER_GROUP);
            } catch (Exception e) {
                log.debug("소비자 그룹이 이미 존재함: {}", e.getMessage());
            }

            startStreamConsumer();

        } catch (Exception e) {
            log.error("Stream 소비자 초기화 실패", e);
        }
    }

    private void startStreamConsumer() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    List<MapRecord<String, String, Object>> records = streamOperations.read(
                            Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                            StreamOffset.create(notificationStreamKey, ReadOffset.lastConsumed())
                    );

                    for (MapRecord<String, String, Object> record : records) {
                        handleStreamNotification(record);
                    }

                } catch (Exception e) {
                    log.error("Stream 메시지 소비 중 오류", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, sseExecutor);
    }

    private void handleStreamNotification(MapRecord<String, String, Object> record) {
        try {
            Map<String, Object> recordValue = record.getValue();
            log.debug("Stream 알림 수신: recordId={}, type={}", record.getId(), recordValue.get("type"));

            NotificationEnvelope<Object> message = convertToNotificationEnvelope(recordValue);

            if (message.data() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.data();
                String targetType = (String) data.get("targetType");

                switch (targetType != null ? targetType : "BROADCAST") {
                    case "BROADCAST" -> sendAll(message);
                    case "USER" -> {
                        Object userIdObj = data.get("userId");
                        if (userIdObj != null) {
                            long memberId = Long.parseLong(userIdObj.toString());
                            send(memberId, message);
                        }
                    }
                    case "USERS" -> {
                        @SuppressWarnings("unchecked")
                        List<Long> memberIds = (List<Long>) data.get("memberIds");
                        if (memberIds != null) {
                            memberIds.forEach(memberId -> send(memberId, message));
                        }
                    }
                    default -> sendAll(message);
                }
            } else {
                sendAll(message);
            }

            streamOperations.acknowledge(notificationStreamKey, CONSUMER_GROUP, record.getId());
            log.debug("메시지 처리 완료: recordId={}", record.getId());

        } catch (Exception e) {
            log.error("Stream 메시지 처리 실패: recordId={}", record.getId(), e);
        }
    }

    private NotificationEnvelope<Object> convertToNotificationEnvelope(Map<String, Object> recordValue) {
        try {
            return new NotificationEnvelope<>(
                    NotificationKind.valueOf((String) recordValue.get("type")),
                    (String) recordValue.get("subject"),
                    (String) recordValue.get("content"),
                    Instant.parse((String) recordValue.get("createdAt")),
                    (String) recordValue.get("link"),
                    recordValue.get("data")
            );
        } catch (Exception e) {
            log.error("NotificationEnvelope 변환 실패", e);
            throw e;
        }
    }

    @PreDestroy
    private void cleanup() {
        log.info("SSE Stream 소비자 종료");
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

        if (lastEventIdHeader != null && !lastEventIdHeader.isBlank()) {
            laneOf(emitterId).execute(() -> resend(memberId, emitterId, emitter, lastEventIdHeader));
        }

        return emitter;
    }

    @Override
    public void send(long memberId, NotificationEnvelope<?> payload) {
        long eventId = nextEventId();
        String json;
        try {
            json = JsonUtils.toJson(objectMapper, payload);
        } catch (Exception e) {
            log.warn("[SSE 전송] payload 직렬화에 실패했습니다. {}번 회원에게 전송하는 실시간알림 전송을 종료합니다.", memberId, e);
            messagesFailed.increment();
            throw new BusinessException(ErrorCode.REALTIME_NOTIFICATION_SEND_FAILED);
        }

        sseRepository.saveEvent(memberId, eventId, json);
        sseRepository.trimEvents(memberId, MAX_REPLAY_PER_MEMBER, eventId - REPLAY_TTL_MS);

        Map<String, SseEmitter> targets = sseRepository.getAllWithIds(memberId);
        if (targets.isEmpty()) {
            log.debug("활성 SSE 송신자 없음; 재생을 위해 이벤트 캐시됨. 회원ID={}, 이벤트ID={}", memberId, eventId);
            return;
        }

        targets.forEach((emitterId, emitter) -> {
            laneOf(emitterId).execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                    messagesSent.increment();

                } catch (IOException ex) {
                    log.warn("전송 실패 - 회원ID={}, 송신자ID={}, 오류={}", memberId, emitterId, ex.toString());
                    messagesFailed.increment();
                    disconnectEmitterWithError(memberId, emitterId, ex);
                    removeLane(emitterId);
                }
            });
        });
    }

    @Override
    public void sendAll(NotificationEnvelope<?> payload) {
        long startTime = clock.millis();

        long eventId = nextEventId();
        String json = JsonUtils.toJson(objectMapper, payload);

        Map<Long, Map<String, SseEmitter>> snapshot = sseRepository.getAllWithIdsGroupedByMember();

        if (snapshot.isEmpty()) {
            log.debug("브로드캐스트 건너뜀: 활성 연결 없음");
            return;
        }

        log.info("브로드캐스트 시작 - 이벤트ID={}, 회원수={}, 총연결수={}",
                eventId, snapshot.size(),
                snapshot.values().stream().mapToLong(Map::size).sum());

        List<Long> memberIdList = new ArrayList<>(snapshot.keySet());
        List<List<Long>> memberChunkList = createChunks(memberIdList);

        List<CompletableFuture<Void>> futureList = memberChunkList.stream()
                .map(chunk -> CompletableFuture.runAsync(() -> {
                    processChunk(chunk, snapshot, eventId, json);
                }, sseExecutor))
                .toList();

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

        long endTime = clock.millis();
        long duration = endTime - startTime;
        log.info("브로드캐스트 완료 - 소요시간={}ms, 대상연결수={}", duration, snapshot.values().stream().mapToLong(Map::size).sum());
    }

    @Override
    public void resend(long memberId, @Nullable String emitterId, SseEmitter emitter, String lastEventIdHeader) {
        final long last;
        try {
            last = Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException ex) {
            log.warn("잘못된 마지막 이벤트ID: {}", lastEventIdHeader);
            return;
        }

        NavigableMap<Long, String> later = sseRepository.eventsAfter(memberId, last);
        if (later.isEmpty()) {
            if (emitterId != null) {
                log.debug("재생 건너뜀 (이후 이벤트 없음) - 회원ID={}, 송신자ID={}, 마지막이벤트ID={}", memberId, emitterId, last);
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
        }

        if (emitterId != null) {
            log.debug("재생 요약 - 회원ID={}, 송신자ID={}, 마지막이벤트ID={}, 최신ID={}, 누락개수={}",
                    memberId, emitterId, last, latestId, missed);
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
                log.debug("전체 연결해제: 완료 실패 (회원ID={}, 송신자ID={}) - {}", memberId, emitterId, ex.toString());
            } finally {
                removeLane(emitterId);
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
            removeLane(emitterId);
        }
    }

    @Override
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
            removeLane(emitterId);
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

        this.messagesFailed = Counter.builder("sse_messages_failed_total")
                .description("전송에 실패한 SSE 메시지 총 개수")
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

    private void processChunk(List<Long> memberChunk,
                              Map<Long, Map<String, SseEmitter>> snapshot,
                              long eventId,
                              String json) {
        String threadName = Thread.currentThread().getName();

        log.debug("청크 처리 시작 - 스레드={}, 회원수={}", threadName, memberChunk.size());

        for (Long memberId : memberChunk) {
            Map<String, SseEmitter> emitters = snapshot.get(memberId);
            if (emitters != null && !emitters.isEmpty()) {
                processMemberConnections(memberId, emitters, eventId, json);
            }
        }

        log.debug("청크 처리 완료 - 스레드={}", threadName);
    }

    private void processMemberConnections(Long memberId,
                                          Map<String, SseEmitter> emitters,
                                          long eventId,
                                          String json) {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            laneOf(emitterId).execute(() -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .id(Long.toString(eventId))
                            .data(json, MediaType.APPLICATION_JSON));
                    messagesSent.increment();

                } catch (IOException ex) {
                    log.warn("브로드캐스트 전송 실패 - 회원ID={}, 송신자ID={}",
                            memberId, emitterId);
                    messagesFailed.increment();
                    disconnectEmitterWithError(memberId, emitterId, ex);
                    removeLane(emitterId);
                }
            });
        }
    }

    private <T> List<List<T>> createChunks(List<T> list) {
        List<List<T>> chunkList = new ArrayList<>();
        for (int i = 0; i < list.size(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, list.size());
            chunkList.add(list.subList(i, end));
        }
        return chunkList;
    }

    private void setEmitter(long memberId, SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("연결 완료 - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
            connectionsCompleted.increment();
        });

        emitter.onTimeout(() -> {
            log.warn("연결 시간초과 - {}", emitterId);
            sseRepository.remove(memberId, emitterId);
            removeLane(emitterId);
            connectionsTimeout.increment();
        });

        emitter.onError(e -> {
            log.warn("연결 오류 - {}: {}", emitterId, e.toString());
            disconnectEmitterWithError(memberId, emitterId, e);
            connectionsError.increment();
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
}