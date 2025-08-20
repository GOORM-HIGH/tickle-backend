package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.dto.PreemptionContext;
import com.profect.tickle.domain.reservation.dto.request.SeatPreemptionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.preemption.PreemptedSeatInfo;
import com.profect.tickle.domain.reservation.dto.response.preemption.SeatPreemptionResponseDto;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeatPreemptionService {

    private static final String UNAVAILABLE_SEAT_MESSAGE = "선택한 좌석 중 선점할 수 없는 좌석이 있습니다.";

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final SeatPreemptionValidator seatPreemptionValidator;

    private static final int PREEMPTION_DURATION_MINUTES = 5; // 5분간 선점

    public SeatPreemptionResponseDto preemptSeats(SeatPreemptionRequestDto request, Long userId) {
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

            return SeatPreemptionResponseDto.failure(
                    UNAVAILABLE_SEAT_MESSAGE,
                    unavailableSeatIds);
        }

        // 4. 전체 좌석 선점
        PreemptionContext context = createPreemptionContext(userId);
        preemptSeats(availableSeats, context);

        // 5. 성공 응답 생성
        List<PreemptedSeatInfo> preemptedSeats = availableSeats.stream()
                .map(this::convertToPreemptedSeatInfo)
                .collect(Collectors.toList());

        // 선점 하고 저장한 좌석들로 응답 뿌려줘야 되지 않을까?
        log.info("🪑좌석 배치 선점 완료! 선점된 좌석 수: {}, 토큰: {}",
                availableSeats.size(), context.getPreemptionToken());

        return SeatPreemptionResponseDto.success(
                context.getPreemptionToken(),
                context.getPreemptedUntil(),
                preemptedSeats,
                String.format("%d개 좌석을 선점했습니다.", availableSeats.size()));
    }

    private PreemptionContext createPreemptionContext(Long userId) {
        String preemptionToken = generatePreemptionToken();
        Instant preemptedAt = Instant.now();
        Instant preemptedUntil = Instant.now()
                .plus(PREEMPTION_DURATION_MINUTES, ChronoUnit.MINUTES);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return PreemptionContext.builder()
                .preemptionToken(preemptionToken)
                .preemptedAt(preemptedAt)
                .preemptedUntil(preemptedUntil)
                .member(member)
                .build();
    }

    private void preemptSeats(List<Seat> availableSeats, PreemptionContext context) {
        Status preemptedStatus = statusProvider.provide(StatusIds.Seat.PREEMPTED);

        for (Seat seat : availableSeats) {
            seat.preempt(
                    context.getPreemptionToken(),
                    context.getPreemptedAt(),
                    context.getPreemptedUntil(),
                    context.getMember(),
                    preemptedStatus
            );
        }

        seatRepository.saveAll(availableSeats);
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