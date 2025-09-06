package com.profect.tickle.domain.event.service.message.subscriber;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.event.EventCoreLockService;
import com.profect.tickle.domain.event.service.event.PostActionsService;
import com.profect.tickle.domain.event.service.message.dto.TicketLockMessage;
import com.profect.tickle.domain.point.entity.PointTarget;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSubscriber {

    private final RedissonClient redissonClient;
    private final EventCoreLockService coreLockService;
    private final PostActionsService postActionsService;

    private static final String TOPIC_NAME = "ticket-event-topic";

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new JsonJacksonCodec());

        topic.addListener(TicketLockMessage.class, (channel, msg) -> {
            log.info("Received ticket event message: {}", msg);

            String lockName = "ticket-lock:" + msg.getEventId();
            RLock lock = redissonClient.getLock(lockName);

            // 최대 5초 대기, 자동 해제 10초
            lock.lockAsync(10, TimeUnit.SECONDS).thenRunAsync(() -> {
                try {
                    // 핵심 로직 적용
                    EventDecision decision = coreLockService.applyCore(msg.getEventId(), msg.getMemberId());

                    // 후속 처리
                    postActionsService.recordPointHistory(msg.getMemberId(), decision.perPrice(), PointTarget.EVENT);

                    if (decision.winner()) {
                        postActionsService.reserveSeatAndCreateReservation(
                                decision.seatId(), msg.getMemberId(), decision.perPrice());
                    }

                } catch (Exception e) {
                    log.error("Error processing ticket event: {}", e.getMessage(), e);
                } finally {
                    lock.unlockAsync();
                }
            });
        });
    }
}