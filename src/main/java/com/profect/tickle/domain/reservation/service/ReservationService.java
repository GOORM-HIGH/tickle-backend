package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.event.reservation.event.ReservationSuccessEvent;
import com.profect.tickle.domain.performance.dto.response.PerformanceServiceDto;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.mapper.PerformanceMapper;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.domain.reservation.dto.request.ReservationCompletionRequestDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationCompletionResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.mapper.ReservationMapper;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final ApplicationEventPublisher eventPublisher;

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;
    private final PointService pointService;
    private final CouponService couponService;
    private final PerformanceMapper performanceMapper;
    private final ReservationMapper reservationMapper;
    private final StatusProvider statusProvider;

    // 예매 생성 메서드
    public ReservationCompletionResponseDto completeReservation(ReservationCompletionRequestDto request) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            // 1. 선점 토큰으로 좌석들 조회 및 검증
            List<Seat> preemptedSeats = validatePreemptedSeats(request.getPreemptionToken(), userId);

            // 2. 금액 검증
//            validatePaymentAmount(preemptedSeats, request);

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
            updateSeatsToReserved(preemptedSeats, reservation, member);

            reservationRepository.save(reservation);

            // 8. 응답 생성
            List<ReservedSeatDto> reservedSeats = preemptedSeats.stream()
                    .map(this::convertToReservedSeatInfo)
                    .collect(Collectors.toList());

            Integer remainingPoints = pointService.getCurrentPoint().credit();

            // 9. 이벤트 생성(예매 성공 알림을 보내기 위함)
            publishReservationSuccessEvent(reservation.getId());

            return ReservationCompletionResponseDto.success(reservation, reservedSeats, remainingPoints);

        } catch (BusinessException e) {
            log.error("Reservation completion failed", e);
            return ReservationCompletionResponseDto.failure(e.getMessage());
        }
    }

    // 예매 성공 이벤트 발행 메서드
    private void publishReservationSuccessEvent(Long reservationId) {
        PerformanceServiceDto performanceServiceDto = performanceMapper.findByReservationId(reservationId); // 예매 공연 정보
        ReservationServiceDto reservationServiceDto = reservationMapper.findById(reservationId) // 예매 정보
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        eventPublisher.publishEvent(new ReservationSuccessEvent(performanceServiceDto, reservationServiceDto));
        log.info("[{}이벤트 발행]", NotificationKind.RESERVATION_SUCCESS);
    }

    private List<Seat> validatePreemptedSeats(String preemptionToken, Long userId) {
        List<Seat> seats = seatRepository.findByPreemptionTokenWithLock(preemptionToken);

        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.PREEMPTION_TOKEN_INVALID);
        }

        Instant now = Instant.now();

        for (Seat seat : seats) {
            if (!userId.equals(seat.getMember().getId())) {
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

//    private void validatePaymentAmount(List<Seat> seats, ReservationCompletionRequestDto request) {
//        Integer calculatedTotal = seats.stream()
//                .mapToInt(Seat::getSeatPrice)
//                .sum();
//
//        if (!calculatedTotal.equals(request.getTotalAmount())) {
//            throw new BusinessException(ErrorCode.RESERVATION_AMOUNT_MISMATCH);
//        }
//    }

    private Reservation createReservation(
            List<Seat> seats,
            Member member,
            Integer price) {

        Performance performance = seats.getFirst().getPerformance();

        Status paidStatus = statusProvider.provide(StatusIds.Reservation.PAID);

        Reservation reservation = Reservation.create(
                member,
                performance,
                paidStatus,
                price
        );

        return reservationRepository.save(reservation);
    }

    private void updateSeatsToReserved(List<Seat> seats, Reservation reservation, Member member) {
        Status reservedStatus = statusProvider.provide(StatusIds.Seat.RESERVED);

        for (Seat seat : seats) {
            seat.assignReservation(reservation);
            seat.assignTo(member);
            seat.assignPreemptionToken(null);
            seat.assignPreemptedAt(null);
            seat.assignPreemptedUntil(null);
            seat.setStatusTo(reservedStatus);
            seat.assignSeatCode(generateSeatCode());
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
    public List<ReservedSeatDto> getSeatListByReservationId(Long reservationId) {
        return reservationMapper.findReservedSeatListByReservationId(reservationId);
    }

    private String generateSeatCode() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuidPart + dateTime;
    }
}
