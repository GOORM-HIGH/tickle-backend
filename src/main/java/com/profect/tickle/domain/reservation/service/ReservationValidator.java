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
            validatePreemptionPermission(userId, seat);
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

    private void validatePreemptionPermission(Long userId, Seat seat) {
        if (isNotSeatOwner(userId, seat)) {
            throw new BusinessException(ErrorCode.PREEMPTION_PERMISSION_DENIED);
        }
    }

    private boolean isNotSeatOwner(Long userId, Seat seat) {
        return !userId.equals(seat.getMember().getId());
    }

    private void validatePreemptionNotExpired(Seat seat, Instant now) {
        // 선점 만료 시간이 없거나, 지난 경우
        if (seat.getPreemptedUntil() == null || seat.getPreemptedUntil().isBefore(now)) {
            throw new BusinessException(ErrorCode.PREEMPTION_EXPIRED);
        }
    }

    private void validateSeatNotAlreadyReserved(Seat seat) {
        // 좌석에 대한 예매가 이미 할당 된 경우
        if (seat.getReservation() != null) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_RESERVED);
        }
    }
}