package com.profect.tickle.domain.reservation.dto.response;

import com.profect.tickle.global.status.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ReservationInfo {
    private Status status;                      // 예매 상태
    private Instant reservedAt;                 // 예매 일시
    private Instant cancelledAt;                // 취소 일시 (취소된 경우)
    private boolean cancellable;                // 취소 가능 여부
    private boolean refundable;                 // 환불 가능 여부
}
