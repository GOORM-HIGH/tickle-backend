// com.profect.tickle.domain.event.stream.consumer.TicketEventWorker
package com.profect.tickle.domain.event.stream.consumer;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.event.EventCoreLockService;
import com.profect.tickle.domain.event.service.event.PostActionsService;
//import com.profect.tickle.domain.event.stream.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.profect.tickle.domain.event.stream.StreamInitializer.GROUP;
import static com.profect.tickle.domain.event.stream.StreamInitializer.STREAM_KEY;
import static com.profect.tickle.domain.point.entity.PointTarget.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventWorker {

    // 동시성/배치/블로킹/재처리 파라미터
    private static final String CONSUMER_PREFIX = "worker-";
    private static final int    POOL_SIZE       = 4;
    private static final int    BATCH           = 16;
    private static final long   BLOCK_MS        = 2_000;
    private static final long   CLAIM_IDLE_MS   = 30_000;
    private static final int    CLAIM_PAGE      = 64;

    private final RedissonClient redisson;
    private final TypedJsonJacksonCodec streamFieldMapCodec;

    private final EventCoreLockService core;
    private final PostActionsService   postActions;
    //private final IdempotencyService   idem;

    private final ExecutorService pool =
            Executors.newFixedThreadPool(POOL_SIZE,
                    new CustomizableThreadFactory("ticket-worker-"));

    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        for (int i = 0; i < POOL_SIZE; i++) {
            final String consumer = CONSUMER_PREFIX + i;
            pool.submit(() -> workLoop(consumer));
        }
        pool.submit(this::reclaimLoop);
    }

    private void workLoop(String consumerName) {
        RStream<String, Object> stream = redisson.getStream(STREAM_KEY, streamFieldMapCodec);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                StreamReadGroupArgs args = StreamReadGroupArgs
                        .greaterThan(StreamMessageId.NEVER_DELIVERED)
                        .count(BATCH)
                        .timeout(Duration.ofMillis(BLOCK_MS));

                Map<StreamMessageId, Map<String, Object>> batch =
                        stream.readGroup(GROUP, consumerName, args);

                if (batch == null || batch.isEmpty()) continue;

                for (var e : batch.entrySet()) {
                    StreamMessageId id = e.getKey();
                    Map<String, Object> fields = e.getValue();

                    Long eventId  = asLong(fields.get("eventId"));
                    Long memberId = asLong(fields.get("memberId"));

                    // 멱등 확인 -> 한 사용자가 중복 참여 가능하므로 멱등 주석
/*                    if (!idem.tryMarkProcessed(eventId, memberId)) {
                        stream.ack(GROUP, id);
                        continue;
                    }*/

                    try {
                        EventDecision d = core.applyCore(eventId, memberId);
                        postActions.recordPointHistory(memberId, d.perPrice(), EVENT);

                        if (d.winner()) {
                            postActions.reserveSeatAndCreateReservation(d.seatId(), memberId, d.perPrice());
                        }

                        stream.ack(GROUP, id); // 성공 시 ACK

                    } catch (Exception ex) {
                        // 실패 시 ACK하지 않음 → PEL에 남아 재시도/클레임 대상
                        log.error("ticket event failed id={} e={}", id, ex.toString(), ex);
                    }
                }

            } catch (Exception e) {
                log.warn("read loop error: {}", e.toString());
                sleepQuiet(300);
            }
        }
    }

    /** PEL 기반 자동 재처리(autoClaim) */
    /** PEL(autoClaim) 기반 재처리 */
    private void reclaimLoop() {
        RStream<String, Object> stream = redisson.getStream(STREAM_KEY, streamFieldMapCodec);
        StreamMessageId start = StreamMessageId.MIN;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                AutoClaimResult<String, Object> res = stream.autoClaim(
                        GROUP,
                        CONSUMER_PREFIX + "reclaimer",
                        CLAIM_IDLE_MS,              // long 값 (30_000)
                        TimeUnit.MILLISECONDS,      // 단위
                        start,                      // 시작 ID
                        CLAIM_PAGE                  // 조회 개수
                );

                Map<StreamMessageId, Map<String, Object>> claimed = res.getMessages();
                start = res.getNextId();

                if (claimed.isEmpty()) {
                    sleepQuiet(2_000);
                    continue;
                }

                for (var e : claimed.entrySet()) {
                    StreamMessageId id = e.getKey();
                    Map<String, Object> fields = e.getValue();

                    Long eventId  = asLong(fields.get("eventId"));
                    Long memberId = asLong(fields.get("memberId"));

                    try {
                        EventDecision d = core.applyCore(eventId, memberId);

                        postActions.recordPointHistory(
                                memberId, d.perPrice(),
                                EVENT);

                        if (d.winner()) {
                            postActions.reserveSeatAndCreateReservation(
                                    d.seatId(), memberId, d.perPrice());
                        }
                        stream.ack(GROUP, id);

                    } catch (Exception ex) {
                        log.error("reclaim failed id={} e={}", id, ex.toString(), ex);
                        // ACK 안 함 → 다음 reclaim 대상
                    }
                }

            } catch (Exception e) {
                log.warn("autoClaim error: {}", e.toString());
                sleepQuiet(2_000);
            }
        }
    }

    // ==== utils ====
    private static Long asLong(Object v) {
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        return (v == null) ? null : Long.valueOf(v.toString());
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}