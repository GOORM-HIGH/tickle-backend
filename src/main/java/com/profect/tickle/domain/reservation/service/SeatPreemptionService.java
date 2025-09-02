package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.reservation.config.SeatPreemptionConfig;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatPreemptionService {

    private static final String UNAVAILABLE_SEAT_MESSAGE = "선택한 좌석 중 선점할 수 없는 좌석이 있습니다.";

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final SeatPreemptionConfig config;

    @Transactional
    public SeatPreemptionResponseDto preemptSeats(SeatPreemptionRequestDto request, Long memberId) {

        validateUserSeatLimits(request, memberId);

        // 1. 좌석 조회 및 선점 가능 여부 확인
        List<Seat> seats = seatRepository.findAllByIdWithLock(request.getSeatIds());
        List<Seat> availableSeats = filterAvailableSeats(seats, request.getPerformanceId());

        // 2. 전체 선점 가능 여부 확인
        if (hasUnavailableSeats(request, availableSeats)) {
            List<Long> unavailableSeatIds = getUnavailableSeatIds(seats, availableSeats);
            return SeatPreemptionResponseDto.failure(UNAVAILABLE_SEAT_MESSAGE, unavailableSeatIds);
        }

        // 3. 전체 좌석 선점
        return executePreemption(memberId, seats);
    }

    private void validateUserSeatLimits(SeatPreemptionRequestDto request, Long memberId) {
        long reservedSeatCount = seatRepository.countReservedSeatsByUserAndPerformance(memberId,
                request.getPerformanceId());

        if (reservedSeatCount >= config.getMaxSeatsPerMember()) {
            throw new BusinessException(
                    String.format("이미 %d개 좌석을 예매했습니다. 더 이상 선점할 수 없습니다.",
                            config.getMaxSeatsPerMember()),
                    ErrorCode.SEAT_LIMIT_EXCEEDED);
        }

        if (reservedSeatCount + request.getSeatIds().size() > config.getMaxSeatsPerMember()) {
            throw new BusinessException(
                    String.format("총 좌석 수 %d개를 초과하여 선점할 수 없습니다. 현재 예매된 좌석: %d",
                            config.getMaxSeatsPerMember(), reservedSeatCount),
                    ErrorCode.SEAT_SELECTION_EXCEEDED
            );
        }
    }

    private List<Seat> filterAvailableSeats(List<Seat> seats, Long performanceId) {
        Instant now = Instant.now();

        return seats.stream()
                .filter(seat -> seat.belongsToPerformance(performanceId))
                .filter(Seat::isAvailableForPreemption)
                .collect(Collectors.toList());
    }

    private boolean hasUnavailableSeats(SeatPreemptionRequestDto request,
            List<Seat> availableSeats) {
        return availableSeats.size() != request.getSeatIds().size();
    }

    private List<Long> getUnavailableSeatIds(List<Seat> allSeats, List<Seat> availableSeats) {
        Set<Long> availableSeatIds = availableSeats.stream()
                .map(Seat::getId)
                .collect(Collectors.toSet());

        return allSeats.stream()
                .map(Seat::getId)
                .filter(id -> !availableSeatIds.contains(id))
                .toList();
    }

    private SeatPreemptionResponseDto executePreemption(Long memberId, List<Seat> seats) {
        try {
            PreemptionContext context = createPreemptionContext(memberId);
            List<Seat> preemptedSeats = performSeatPreemption(seats, context);

            List<PreemptedSeatInfo> preemptedSeatInfos = convertToPreemptedSeatInfos(
                    preemptedSeats);

            log.info("좌석 선점 완료 - 사용자: {}, 선점된 좌석 수: {}, 토큰: {}, 만료 시간: {}",
                    memberId, preemptedSeats.size(), context.getPreemptionToken(),
                    context.getPreemptedUntil());

            return SeatPreemptionResponseDto.success(
                    context.getPreemptionToken(),
                    context.getPreemptedUntil(),
                    preemptedSeatInfos,
                    String.format("%d개 좌석을 선점했습니다.", preemptedSeats.size()));

        } catch (Exception e) {
            log.error("좌석 선점 중 오류 발생 - 사용자: {}, 좌석 IDs: {}", memberId,
                    seats.stream().map(Seat::getId).collect(Collectors.toList()), e);
            throw new BusinessException(e.getMessage(), ErrorCode.SEAT_PREEMPTION_EXCEPTION);
        }

    }

    private PreemptionContext createPreemptionContext(Long userId) {
        String preemptionToken = generatePreemptionToken();
        Instant preemptedAt = Instant.now();
        Instant preemptedUntil = Instant.now()
                .plus(config.getPreemptionDurationMinutes(), ChronoUnit.MINUTES);
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return PreemptionContext.builder()
                .preemptionToken(preemptionToken)
                .preemptedAt(preemptedAt)
                .preemptedUntil(preemptedUntil)
                .member(member)
                .build();
    }

    private List<Seat> performSeatPreemption(List<Seat> seats, PreemptionContext context) {
        Status preemptedStatus = statusProvider.provide(StatusIds.Seat.PREEMPTED);

        seats.forEach(seat -> seat.preempt(
                context.getPreemptionToken(),
                context.getPreemptedAt(),
                context.getPreemptedUntil(),
                context.getMember(),
                preemptedStatus
        ));

        return seatRepository.saveAll(seats);
    }

    private String generatePreemptionToken() {
        return UUID.randomUUID().toString();
    }

    private List<PreemptedSeatInfo> convertToPreemptedSeatInfos(List<Seat> preemptedSeats) {
        return preemptedSeats.stream()
                .map(this::convertToPreemptedSeatInfo)
                .toList();
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