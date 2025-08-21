package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReservationValidator {

    public void validatePreemptedSeats(List<Seat> seats, Long userId) {
        validatePreemptionToken(seats);

        Instant now = Instant.now();
        for (Seat seat : seats) {
            validateSeatOwnership(userId, seat);
            validatePreemptionNotExpired(seat, now);
            validateSeatNotAlreadyReserved(seat);
        }
    }

    public void validatePaymentAmount(int finalAmount, int requestTotalAmount) {
        if (finalAmount != requestTotalAmount) {
            throw new BusinessException(ErrorCode.RESERVATION_AMOUNT_MISMATCH);
        }
    }

    private void validatePreemptionToken(List<Seat> seats) {
        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.PREEMPTION_TOKEN_INVALID);
        }
    }

    private void validateSeatOwnership(Long userId, Seat seat) {
        if (!seat.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PREEMPTION_PERMISSION_DENIED);
        }
    }

    private void validatePreemptionNotExpired(Seat seat, Instant now) {
        // 선점이 만료 경우
        if (seat.isPreemptionExpired()) {
            throw new BusinessException(ErrorCode.PREEMPTION_EXPIRED);
        }
    }

    private void validateSeatNotAlreadyReserved(Seat seat) {
        // 좌석이 이미 예매된 경우
        if (seat.isAlreadyReserved()) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_RESERVED);
        }
    }
}