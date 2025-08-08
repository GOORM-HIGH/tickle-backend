package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.mapper.MemberMapper;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.notification.event.reservation.event.ReservationSuccessEvent;
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.domain.reservation.dto.request.ReservationCompletionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationCompletionResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.ReservationStatus;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.entity.SeatStatus;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final ApplicationEventPublisher eventPublisher;

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final StatusRepository statusRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;
    private final PointService pointService;
    private final CouponService couponService;
    private final PerformanceMapper performanceMapper;
    private final MemberMapper memberMapper;

    public ReservationCompletionResponseDto completeReservation(ReservationCompletionRequestDto request) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            // 1. 선점 토큰으로 좌석들 조회 및 검증
            List<Seat> preemptedSeats = validatePreemptedSeats(request.getPreemptionToken(), userId);

            // 2. 금액 검증
            validatePaymentAmount(preemptedSeats, request);

            // 3. 쿠폰 할인 계산
            Integer discountAmount = 0;
            if (request.getCouponId() != null) {
                discountAmount = couponService.calculateCouponDiscount(request.getCouponId(), userId, request.getTotalAmount());
            }

            Integer finalAmount = request.getTotalAmount() - discountAmount;

            // 4. 포인트 충분성 검증
            int currentPoints = pointService.getCurrentPoint().credit();
            if (currentPoints < finalAmount) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
            }

            // 5. 쿠폰 사용 처리 (있는 경우)
            if (request.getCouponId() != null) {
                couponService.useCoupon(request.getCouponId(), userId);
            }

            // 6. 포인트 차감 // 아래 로직 추후 리팩터링 예정
            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Point point = member.deductPoint(finalAmount, PointTarget.RESERVATION);

            pointRepository.save(point);


            // 7. 예매 생성
            Reservation reservation = createReservation(preemptedSeats, member, finalAmount);

            // 7. 좌석들을 예매 완료 상태로 변경
            updateSeatsToReserved(preemptedSeats, reservation);

            // 8. 응답 생성
            List<ReservedSeatDto> reservedSeats = preemptedSeats.stream()
                    .map(this::convertToReservedSeatInfo)
                    .collect(Collectors.toList());

            Integer remainingPoints = pointService.getCurrentPoint().credit();

            // 9. 이벤트 생성(예매 성공 알림을 보내기 위함)
            PerformanceDto reservedPerformance = performanceMapper.findByReservationId(); // 예매한 공연
            Member siginMember = memberRepository.findById(userId) // 예매한 유저
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.MEMBER_NOT_FOUND.getMessage(),
                            ErrorCode.MEMBER_NOT_FOUND)
                    );
            eventPublisher.publishEvent(new ReservationSuccessEvent(reservedPerformance, siginMember, reservation));

            return ReservationCompletionResponseDto.success(reservation, reservedSeats, remainingPoints);

        } catch (BusinessException e) {
            log.error("Reservation completion failed", e);
            return ReservationCompletionResponseDto.failure(e.getMessage());
        }
    }

    private List<Seat> validatePreemptedSeats(String preemptionToken, Long userId) {
        List<Seat> seats = seatRepository.findByPreemptionTokenWithLock(preemptionToken);

        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.PREEMPTION_TOKEN_INVALID);
        }

        Instant now = Instant.now();

        for (Seat seat : seats) {
            if (!userId.equals(seat.getPreemptUserId())) {
                throw new BusinessException(ErrorCode.PREEMPTION_PERMISSION_DENIED);
            }

            if (seat.getPreemptedUntil() == null || seat.getPreemptedUntil().isBefore(now)) {
                throw new BusinessException(ErrorCode.PREEMPTION_EXPIRED);
            }

            if (seat.getReservation() != null) {
                throw new BusinessException(ErrorCode.RESERVATION_ALREADY_RESERVED);
            }
        }

        return seats;
    }

    private void validatePaymentAmount(List<Seat> seats, ReservationCompletionRequestDto request) {
        Integer calculatedTotal = seats.stream()
                .mapToInt(Seat::getSeatPrice)
                .sum();

        if (!calculatedTotal.equals(request.getTotalAmount())) {
            throw new BusinessException(ErrorCode.RESERVATION_AMOUNT_MISMATCH);
        }
    }

    private Reservation createReservation(
            List<Seat> seats,
            Member member,
            Integer price) {

        Performance performance = seats.get(0).getPerformance();

        Status paidStatus = statusRepository.findById(ReservationStatus.PAID.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        Reservation reservation = Reservation.create(
                member,
                performance,
                paidStatus,
                price
        );

        return reservationRepository.save(reservation);
    }

    private void updateSeatsToReserved(List<Seat> seats, Reservation reservation) {
        Status reservedStatus = statusRepository.findById(SeatStatus.RESERVED.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        for (Seat seat : seats) {
            seat.assignReservation(reservation);
            seat.assignTo(null);
            seat.assignPreemptionToken(null);
            seat.assignPreemptedAt(null);
            seat.assignPreemptedUntil(null);
            seat.setStatusTo(reservedStatus);
            seat.assignSeatCode(generateReservationCode());
        }

        seatRepository.saveAll(seats);
    }

    private ReservedSeatDto convertToReservedSeatInfo(Seat seat) {
        return ReservedSeatDto.builder()
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatGrade(seat.getSeatGrade())
                .seatPrice(seat.getSeatPrice())
                .build();
    }

    // 예약Id로 자석정보 조회
    private List<ReservedSeatInfo> getSeatListByReservationId(Long reservationId) {
        return List.of(); // TODO: seatService로 실제 구현
    }

    private String generateReservationCode() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuidPart + dateTime;
    }
}
