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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CachedSeatPreemptionService {

    private static final String UNAVAILABLE_SEAT_MESSAGE = "선택한 좌석 중 선점할 수 없는 좌석이 있습니다.";
    private static final String PREEMPTED_SEAT_KEY = "preempted:seat:";

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final SeatPreemptionConfig config;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public SeatPreemptionResponseDto preemptSeats(SeatPreemptionRequestDto request, Long memberId) {

        validateUserSeatLimits(request, memberId);

        // 1. 캐시에서 이미 선점된 좌석 확인 (빠른 실패)
        List<Long> preemptedSeatIds = checkPreemptedSeatsInCache(request.getSeatIds());
        if (!preemptedSeatIds.isEmpty()) {
            log.info("캐시에서 선점된 좌석 발견으로 빠른 응답 - 좌석: {}", preemptedSeatIds);
            return SeatPreemptionResponseDto.failure(UNAVAILABLE_SEAT_MESSAGE, preemptedSeatIds);
        }

        // 2. DB에서 좌석 조회 및 선점 가능 여부 확인
        List<Seat> seats = seatRepository.findAllByIdWithLock(request.getSeatIds());
        List<Seat> availableSeats = filterAvailableSeats(seats, request.getPerformanceId());

        // 3. 전체 선점 가능 여부 확인
        if (hasUnavailableSeats(request, availableSeats)) {
            List<Long> unavailableSeatIds = getUnavailableSeatIds(seats, availableSeats);
            // 선점 불가능한 좌석들을 캐시에 저장
            saveUnavailableSeatsToCache(unavailableSeatIds);
            return SeatPreemptionResponseDto.failure(UNAVAILABLE_SEAT_MESSAGE, unavailableSeatIds);
        }

        // 4. 전체 좌석 선점 및 캐시 저장
        return executePreemption(memberId, seats);
    }

    /**
     * 캐시에서 이미 선점된 좌석들 확인
     */
    private List<Long> checkPreemptedSeatsInCache(List<Long> seatIds) {
        return seatIds.stream()
                .filter(seatId -> redisTemplate.hasKey(PREEMPTED_SEAT_KEY + seatId))
                .collect(Collectors.toList());
    }

    /**
     * 선점 불가능한 좌석들을 캐시에 저장
     */
    private void saveUnavailableSeatsToCache(List<Long> seatIds) {
        Duration cacheDuration = Duration.ofMinutes(config.getPreemptionDurationMinutes() + 1);

        seatIds.forEach(seatId -> {
            String cacheKey = PREEMPTED_SEAT_KEY + seatId;
            redisTemplate.opsForValue().set(cacheKey, "PREEMPTED", cacheDuration);
        });

        log.debug("선점 불가능한 좌석 {}개를 캐시에 저장", seatIds.size());
    }

    /**
     * 선점 성공한 좌석들을 캐시에 저장
     */
    private void savePreemptedSeatsToCache(List<Seat> seats, Instant preemptedUntil) {
        Duration cacheDuration = Duration.between(Instant.now(), preemptedUntil).plusMinutes(1);

        seats.forEach(seat -> {
            String cacheKey = PREEMPTED_SEAT_KEY + seat.getId();
            redisTemplate.opsForValue().set(cacheKey, seat.getPreemptionToken(), cacheDuration);
        });

        log.debug("선점된 좌석 {}개를 캐시에 저장", seats.size());
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

            // 선점된 좌석들을 캐시에 저장
            savePreemptedSeatsToCache(preemptedSeats, context.getPreemptedUntil());

            List<PreemptedSeatInfo> preemptedSeatInfos = convertToPreemptedSeatInfos(preemptedSeats);

            log.info("좌석 선점 완료 - 사용자: {}, 선점된 좌석 수: {}, 토큰: {}",
                    memberId, preemptedSeats.size(), context.getPreemptionToken());

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