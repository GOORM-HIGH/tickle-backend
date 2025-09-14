/*
package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.event.entity.TicketApplied;
import com.profect.tickle.domain.point.entity.PointTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAppliedListener {

    private final PostActionsService post;
    private final RedissonClient redisson;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(TicketAppliedDto e) {
        // seatId를 기준으로 락을 걸면, 같은 좌석에 동시에 접근하는 요청을 막을 수 있음
        String lockKey = "seat-lock:" + e.seatId();
        RLock lock = redisson.getLock(lockKey);

        try {
            // 락 획득 최대 5초, 획득하면 10초 동안 유지
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("[TicketAppliedListener] lock acquired member={}, seat={}", e.memberId(), e.seatId());

                    // 1️⃣ 포인트 적립
                    post.recordPointHistory(e.memberId(), e.perPrice(), e.target());

                    // 2️⃣ 좌석 예약
                    if (e.isWinner() && e.seatId() != null) {
                        post.reserveSeatAndCreateReservation(e.seatId(), e.memberId(), e.accrued());
                    }

                } finally {
                    lock.unlock();
                    log.info("[TicketAppliedListener] lock released member={}, seat={}", e.memberId(), e.seatId());
                }
            } else {
                log.warn("[TicketAppliedListener] could not acquire lock, member={}, seat={}", e.memberId(), e.seatId());
                // 락 획득 실패 시 재시도 로직을 추가하거나 예외 처리 가능
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("[TicketAppliedListener] lock interrupted, member={}, seat={}", e.memberId(), e.seatId(), ex);
        }
    }
}*/
