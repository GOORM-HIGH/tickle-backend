package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequest;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatPreemptionValidator {

    private final SeatRepository seatRepository;

    public void validateRequest(SeatPreemptionRequest request, Long userId) {
        // 1. 사용자의 기존 선점 좌석 수 확인
        int currentPreemptedCount = seatRepository.countByPreemptUserIdAndPreemptedUntilAfter(
                userId, Instant.now());

        int requestedCount = request.getSeatIds().size();

        if (currentPreemptedCount + requestedCount > 5) {
            throw new IllegalArgumentException(
                    String.format("좌석은 최대 5개까지 선점할 수 있습니다. (현재: %d개, 요청: %d개)",
                            currentPreemptedCount, requestedCount));
        }

        // 2. 중복 선점 확인
        List<Long> alreadyPreemptedByUser = seatRepository
                .findPreemptedSeatIdsByUserAndSeatIds(userId, request.getSeatIds());

        if (!alreadyPreemptedByUser.isEmpty()) {
            throw new IllegalArgumentException("이미 선점한 좌석이 포함되어 있습니다.");
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
