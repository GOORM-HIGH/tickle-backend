/*
package com.profect.tickle.domain.event.service.lock;

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
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockManager {

    private final RedissonClient redisson;
    private final EventCoreLockService core;
    private final PostActionsService post;

    // 이미 구독 중인 채널 관리
    private final ConcurrentHashMap<Long, Boolean> subscribedChannels = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 공용 구독 채널 (모든 이벤트 메시지를 수신)
        RTopic commonTopic = redisson.getTopic("ticketLockChannel");
        commonTopic.addListener(TicketLockMessage.class, (ch, msg) -> handleMessage(msg));
    }

    private void handleMessage(TicketLockMessage msg) {
        // 메시지 수신 시 락 시도
        attemptLock(msg);
    }

    public void attemptLock(TicketLockMessage msg) {
        String lockKey = "ticket-lock:" + msg.eventId();
        RLock lock = redisson.getLock(lockKey);

        lock.tryLockAsync()
                .thenAccept(acquired -> {
                    if (acquired) {
                        log.info("[LockManager] 락 획득: member={}, event={}", msg.memberId(), msg.eventId());
                        processCore(msg)
                                .thenRun(() -> {
                                    lock.unlockAsync().thenRun(() -> {
                                        log.info("[LockManager] 락 해제: member={}, event={}", msg.memberId(), msg.eventId());
                                        // 재시도 필요 시 다른 구독자에게 알림
                                        publishRetry(msg);
                                    });
                                });
                    } else {
                        log.info("[LockManager] 락 획득 실패: member={}, event={}", msg.memberId(), msg.eventId());
                        publishRetry(msg); // 락 실패 메시지 발행
                    }
                })
                .exceptionally(ex -> {
                    log.error("[LockManager] 락 시도 중 오류: member={}, event={}", msg.memberId(), msg.eventId(), ex);
                    return null;
                });
    }

    private java.util.concurrent.CompletableFuture<Void> processCore(TicketLockMessage msg) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            // 핵심 로직
            EventDecision result = core.applyCore(msg.eventId(), msg.memberId());
            // 포인트/좌석 처리
            postAfterCore(result);
        });
    }

    private void postAfterCore(EventDecision result) {
        // 포인트 기록
        post.recordPointHistory(result.memberId(), result.perPrice(), PointTarget.EVENT);

        // 승자 좌석 예약
        if (result.seatId() != null)
            post.reserveSeatAndCreateReservation(result.seatId(), result.memberId(), result.accrued());
    }

    private void publishRetry(TicketLockMessage msg) {
        String channelName = "ticketLockChannel";
        RTopic topic = redisson.getTopic(channelName);
        topic.publish(msg);
    }
}*/
