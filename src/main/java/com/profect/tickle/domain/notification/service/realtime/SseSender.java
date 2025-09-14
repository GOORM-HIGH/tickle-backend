package com.profect.tickle.domain.notification.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender implements RealtimeSender {

    // properties
    private static final int CHUNK_SIZE = 500;
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

    // metrics
    private Counter connectionsCreated;     // SSE 연결 생성 총 횟수
    private Counter connectionsCompleted;   // SSE 연결 정상 완료 총 횟수
    private Counter connectionsTimeout;     // SSE 연결 타임아웃 총 횟수
    private Counter connectionsError;       // SSE 연결 에러 발생 총 횟수
    private Counter messagesSent;           // SSE 메시지 전송 성공 총 횟수
    private Counter messagesFailed;         // SSE 메시지 전송 실패 총 횟수

    @Override
    public SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventIdHeader) {
        // 연결마다 고유 emitterId 생성
        Instant connectedAt = clock.instant();
        UUID uuid = uuidSupplier.get();
        String emitterId = memberId + "_" + connectedAt.toEpochMilli() + "_" + uuid;

        log.info("SSE 연결 - 회원ID={}, 송신자ID={}", memberId, emitterId);

        // 연결 생성 메트릭 증가
        connectionsCreated.increment();

        // emitter 생성
        SseEmitter emitter = new SseEmitter(notificationProperty.sseTimeout().toMillis());
        setEmitter(memberId, emitter, emitterId);
        sseRepository.save(memberId, emitterId, emitter);

        // 초기 핑(Last-Event-ID 체인 시작)
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
            log.debug("활성¸ SSE 송신자 없음; 재생을 위해 이벤트 캐시됨. 회원ID={}, 이벤트ID={}", memberId, eventId);
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
        // 브로드캐스트 처리 시간 측정 시작
        long startTime = clock.millis();

        // 브로드캐스트용 고유 이벤트 ID 생성 및 페이로드 JSON 직렬화
        long eventId = nextEventId();
        String json = JsonUtils.toJson(objectMapper, payload);

        // 현재 시점의 모든 회원별 SSE 연결 상태 스냅샷 조회
        Map<Long, Map<String, SseEmitter>> snapshot = sseRepository.getAllWithIdsGroupedByMember();

        // 활성 연결이 없는 경우 브로드캐스트 중단
        if (snapshot.isEmpty()) {
            log.debug("브로드캐스트 건너뜀: 활성 연결 없음");
            return;
        }

        // 브로드캐스트 시작 로그 출력 (이벤트 ID, 대상 회원 수, 총 연결 수)
        log.info("브로드캐스트 시작 - 이벤트ID={}, 회원수={}, 총연결수={}",
                eventId, snapshot.size(),
                snapshot.values().stream().mapToLong(Map::size).sum());

        // 회원 ID 목록을 추출하여 병렬 처리용 청크로 분할
        List<Long> memberIdList = new ArrayList<>(snapshot.keySet());
        List<List<Long>> memberChunkList = createChunks(memberIdList);

        // 각 청크를 Virtual Thread에서 병렬로 처리하기 위한 CompletableFuture 생성
        List<CompletableFuture<Void>> futureList = memberChunkList.stream()
                .map(chunk -> CompletableFuture.runAsync(() -> {
                    processChunk(chunk, snapshot, eventId, json);
                }, sseExecutor))
                .toList();

        // 모든 청크의 병렬 처리 완료까지 대기
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

        // 브로드캐스트 완료 시간 측정 및 처리 결과 로그 출력
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
            // 재전송 중 끊기면 콜백에서 처리
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

    @PostConstruct
    private void initMetrics() {
        // Counter 메트릭 초기화
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
