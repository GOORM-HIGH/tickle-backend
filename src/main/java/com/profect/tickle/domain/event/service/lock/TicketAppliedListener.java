package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.point.entity.PointTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAppliedListener {
    private final PostActionsService post;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(TicketApplied e) {
        log.info("[TicketAppliedListener] start member={}, winner={}, seatId={}, perPrice={}, accrued={}",
                e.memberId(), e.isWinner(), e.seatId(), e.perPrice(), e.accrued());
        try {
            post.recordPointHistory(e.memberId(), e.perPrice(), e.target());
            if (e.isWinner() && e.seatId() != null) {
                post.reserveSeatAndCreateReservation(e.seatId(), e.memberId(), e.accrued());
            }
            log.info("[TicketAppliedListener] done member={}", e.memberId());
        } catch (Exception ex) {
            log.error("[TicketAppliedListener] failed member={} reason={}", e.memberId(), ex.toString(), ex);

        }
    }
}