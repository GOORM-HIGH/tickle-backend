package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequestDto;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatPreemptionValidator {

    private final SeatRepository seatRepository;

    public void validateRequest(SeatPreemptionRequestDto request, Long userId) {
        // 1. 사용자의 기존 선점 좌석 수 확인
        int currentPreemptedCount = seatRepository.countByPreemptUserIdAndPreemptedUntilAfter(
                userId, Instant.now());

        int requestedCount = request.getSeatIds().size();

        if (currentPreemptedCount + requestedCount > 5) {
            throw new BusinessException(ErrorCode.PREEMPTION_LIMIT_EXCEEDED);
        }

        // 2. 중복 선점 확인
        List<Long> alreadyPreemptedByUser = seatRepository
                .findPreemptedSeatIdsByUserAndSeatIds(userId, request.getSeatIds());

        if (!alreadyPreemptedByUser.isEmpty()) {
            throw new BusinessException(ErrorCode.PREEMPTION_DUPLICATE_SEAT);
        }
    }

    public List<Seat> filterAvailableSeats(List<Seat> seats, Long performanceId) {
        Instant now = Instant.now();

        return seats.stream()
                .filter(seat -> seat.getPerformance().getId().equals(performanceId))
                .filter(seat -> seat.getReservation() == null) // 예약되지 않은 좌석
                .filter(seat -> seat.getPreemptedUntil() == null || seat.getPreemptedUntil().isBefore(now)) // 선점되지 않았거나 만료된 좌석
                .collect(Collectors.toList());
    }
}
