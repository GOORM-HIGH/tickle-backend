package com.profect.tickle.domain.event.service.lock;

import com.profect.tickle.domain.point.entity.PointTarget;

public record TicketApplied(
        Long memberId,
        Long seatId,
        short perPrice,
        int accrued,
        PointTarget target,
        boolean isWinner
) {
}
