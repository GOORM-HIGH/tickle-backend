// com.profect.tickle.domain.event.stream.consumer.TicketEventWorker
package com.profect.tickle.domain.event.stream.consumer;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.event.EventCoreLockService;
import com.profect.tickle.domain.event.service.event.PostActionsService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.profect.tickle.domain.event.stream.StreamInitializer.GROUP;
import static com.profect.tickle.domain.event.stream.StreamInitializer.STREAM_KEY;
import static com.profect.tickle.domain.point.entity.PointTarget.EVENT;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventWorker {

    // ====== 튜닝 파라미터 ======
    private static final String CONSUMER_PREFIX = "worker-";
    private static final int    POOL_SIZE       = 4;      // 동시에 몇 개의 컨슈머 루프를 돌릴지
    private static final int    BATCH           = 16;     // readGroup batch size
    private static final long   BLOCK_MS        = 2_000;  // readGroup block timeout
    private static final long   CLAIM_IDLE_MS   = 30_000; // autoClaim idle 기준
    private static final int    CLAIM_PAGE      = 64;     // autoClaim page size

    // ====== 주입 ======
    private final Executor eventExecutor;   // VirtualThreadTaskExecutor("ticket-worker-")
    private final RedissonClient redisson;
    private final EventCoreLockService core;
    private final PostActionsService   postActions;

    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        for (int i = 0; i < POOL_SIZE; i++) {
            final String consumer = CONSUMER_PREFIX + i;
            eventExecutor.execute(() -> workLoop(consumer));
        }
        eventExecutor.execute(this::reclaimLoop);
    }

    private void workLoop(String consumerName) {
        // ✅ StringCodec으로 명시
        RStream<String, String> stream = redisson.getStream(STREAM_KEY, StringCodec.INSTANCE);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                StreamReadGroupArgs args = StreamReadGroupArgs
                        .greaterThan(StreamMessageId.NEVER_DELIVERED)
                        .count(BATCH)
                        .timeout(Duration.ofMillis(BLOCK_MS));

                Map<StreamMessageId, Map<String, String>> batch =
                        stream.readGroup(GROUP, consumerName, args);

                if (batch == null || batch.isEmpty()) {
                    continue;
                }

                for (var e : batch.entrySet()) {
                    StreamMessageId id = e.getKey();
                    Map<String, String> fields = e.getValue();

                    Long eventId  = asLong(fields.get("eventId"));
                    Long memberId = asLong(fields.get("memberId"));

                    try {
                        EventDecision d = core.applyCore(eventId, memberId);
                        postActions.recordPointHistory(memberId, d.perPrice(), EVENT);

                        if (d.winner()) {
                            postActions.reserveSeatAndCreateReservation(d.seatId(), memberId, d.perPrice());
                        }
                        stream.ack(GROUP, id); // 성공 시 ACK

                    } catch (Exception ex) {
                        // 실패 시 ACK하지 않음 → PEL 남겨 autoClaim 대상
                        log.error("ticket event failed id={} e={}", id, ex.toString(), ex);
                    }
                }

            } catch (Exception e) {
                log.warn("read loop error: {}", e.toString());
                sleepQuiet(200); // 아주 짧은 백오프
            }
        }
    }

    /** PEL(autoClaim) 기반 재처리 */
    private void reclaimLoop() {
        // ✅ StringCodec으로 명시
        RStream<String, String> stream = redisson.getStream(STREAM_KEY, StringCodec.INSTANCE);
        StreamMessageId start = StreamMessageId.MIN;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                AutoClaimResult<String, String> res = stream.autoClaim(
                        GROUP,
                        CONSUMER_PREFIX + "reclaimer",
                        CLAIM_IDLE_MS, TimeUnit.MILLISECONDS,
                        start,
                        CLAIM_PAGE
                );

                Map<StreamMessageId, Map<String, String>> claimed = res.getMessages();
                start = res.getNextId();

                if (claimed.isEmpty()) {
                    sleepQuiet(2_000);
                    continue;
                }

                for (var e : claimed.entrySet()) {
                    StreamMessageId id = e.getKey();
                    Map<String, String> fields = e.getValue();

                    Long eventId  = asLong(fields.get("eventId"));
                    Long memberId = asLong(fields.get("memberId"));

                    try {
                        EventDecision d = core.applyCore(eventId, memberId);
                        postActions.recordPointHistory(memberId, d.perPrice(), EVENT);
                        if (d.winner()) {
                            postActions.reserveSeatAndCreateReservation(d.seatId(), memberId, d.perPrice());
                        }
                        stream.ack(GROUP, id);

                    } catch (Exception ex) {
                        log.error("reclaim failed id={} e={}", id, ex.toString(), ex);
                    }
                }

            } catch (Exception e) {
                log.warn("autoClaim error: {}", e.toString());
                sleepQuiet(2_000);
            }
        }
    }
    
    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping TicketEventWorker...");
        if (eventExecutor instanceof java.util.concurrent.ExecutorService es) {
            es.shutdownNow(); // 또는 graceful 종료: es.shutdown()
            log.info("TicketEventWorker executor shut down.");
        }
    }
    // utils
    private static Long asLong(Object v) {
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        return (v == null) ? null : Long.valueOf(v.toString());
    }
}