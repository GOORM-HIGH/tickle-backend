package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequest;
import com.profect.tickle.domain.reservation.dto.response.PreemptedSeatInfo;
import com.profect.tickle.domain.reservation.dto.response.SeatPreemptionResponse;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.entity.SeatStatus;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatPreemptionService {

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;
    private final SeatPreemptionValidator seatPreemptionValidator;

    private static final int PREEMPTION_DURATION_MINUTES = 5; // 5분간 선점

    public SeatPreemptionResponse preemptSeats(SeatPreemptionRequest request, Long userId) {
        // 1. 기본 검증
        seatPreemptionValidator.validateRequest(request, userId);

        // 2. 좌석 조회 및 선점 가능 여부 확인
        List<Seat> seats = seatRepository.findAllByIdWithLock(request.getSeatIds());
        List<Seat> availableSeats = seatPreemptionValidator.filterAvailableSeats(seats, request.getPerformanceId());

        // 3. 전체 선점 가능 여부 확인
        if (availableSeats.size() != request.getSeatIds().size()) {
            List<Long> unavailableSeatIds = seats.stream()
                    .filter(seat -> !availableSeats.contains(seat))
                    .map(Seat::getId)
                    .collect(Collectors.toList());

            return SeatPreemptionResponse.failure(
                    "선택한 좌석 중 선점할 수 없는 좌석이 있습니다.",
                    unavailableSeatIds);
        }

        // 4. 전체 선점 실행
        String preemptionToken = generatePreemptionToken();
        Instant preemptedUntil = Instant.now().plus(PREEMPTION_DURATION_MINUTES, ChronoUnit.MINUTES);

        for (Seat seat : availableSeats) {
            preemptSeat(seat, userId, preemptionToken, preemptedUntil);
        }

        // 5. 성공 응답 생성
        List<PreemptedSeatInfo> preemptedSeats = availableSeats.stream()
                .map(this::convertToPreemptedSeatInfo)
                .collect(Collectors.toList());

        return SeatPreemptionResponse.success(
                preemptionToken,
                preemptedUntil,
                preemptedSeats,
                String.format("%d개 좌석을 선점했습니다.", availableSeats.size()));
    }

    private void preemptSeat(Seat seat, Long userId, String preemptionToken, Instant preemptedUntil) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        seat.assignTo(member);
        seat.assignPreemptionToken(preemptionToken);
        seat.assignPreemptedAt(Instant.now());
        seat.assignPreemptedUntil(preemptedUntil);

        // DB status 선점중으로 변경 (ID: 12)
        Status preempted = statusRepository.findById(SeatStatus.PREEMPTED.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        seat.setStatusTo(preempted);

        seatRepository.save(seat);
    }

    private String generatePreemptionToken() {
        return UUID.randomUUID().toString();
    }

    private PreemptedSeatInfo convertToPreemptedSeatInfo(Seat seat) {
        return PreemptedSeatInfo.builder()
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatGrade(seat.getSeatGrade())
                .seatPrice(seat.getSeatPrice())
                .build();
    }
}