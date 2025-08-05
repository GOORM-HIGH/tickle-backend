package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.domain.reservation.dto.request.ReservationCompletionRequest;
import com.profect.tickle.domain.reservation.dto.response.ReservationCompletionResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservedSeatInfo;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final StatusRepository statusRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;
    private final PointService pointService;
    private final CouponService couponService;

    public ReservationCompletionResponse completeReservation(ReservationCompletionRequest request) {

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
                    .orElseThrow();

            Point point = member.deductPoint(finalAmount, PointTarget.RESERVATION);

            pointRepository.save(point);


            // 7. 예매 생성
            Reservation reservation = createReservation(preemptedSeats, member, finalAmount);

            // 7. 좌석들을 예매 완료 상태로 변경
            updateSeatsToReserved(preemptedSeats, reservation);

            // 8. 응답 생성
            List<ReservedSeatInfo> reservedSeats = preemptedSeats.stream()
                    .map(this::convertToReservedSeatInfo)
                    .collect(Collectors.toList());

            Integer remainingPoints = pointService.getCurrentPoint().credit();

            return ReservationCompletionResponse.success(reservation, reservedSeats, remainingPoints);

        } catch (Exception e) {
            log.error("Reservation completion failed", e);
            return ReservationCompletionResponse.failure(e.getMessage());
        }
    }

    private List<Seat> validatePreemptedSeats(String preemptionToken, Long userId) {
        List<Seat> seats = seatRepository.findByPreemptionTokenWithLock(preemptionToken);

        if (seats.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 선점 토큰입니다.");
        }

        Instant now = Instant.now();

        for (Seat seat : seats) {
            if (!userId.equals(seat.getPreemptUserId())) {
                throw new IllegalArgumentException("선점 권한이 없습니다.");
            }

            if (seat.getPreemptedUntil() == null || seat.getPreemptedUntil().isBefore(now)) {
                throw new IllegalArgumentException("선점 시간이 만료되었습니다.");
            }

            if (seat.getReservation() != null) {
                throw new IllegalArgumentException("이미 예매된 좌석이 포함되어 있습니다.");
            }
        }

        return seats;
    }

    private void validatePaymentAmount(List<Seat> seats, ReservationCompletionRequest request) {
        Integer calculatedTotal = seats.stream()
                .mapToInt(Seat::getSeatPrice)
                .sum();

        if (!calculatedTotal.equals(request.getTotalAmount())) {
            throw new IllegalArgumentException("총 금액이 일치하지 않습니다.");
        }
    }

    private Reservation createReservation(
            List<Seat> seats,
            Member member,
            Integer price) {

        Performance performance = seats.get(0).getPerformance();

        Status paidStatus = statusRepository.findById(9L)
                .orElseThrow();

        Reservation reservation = Reservation.create(
                member,
                performance,
                paidStatus,
                price
        );

        return reservationRepository.save(reservation);
    }

    private void updateSeatsToReserved(List<Seat> seats, Reservation reservation) {
        Status reservedStatus = statusRepository.findById(13L)
                .orElseThrow(() -> new IllegalStateException("예매완료 상태를 찾을 수 없습니다."));

        for (Seat seat : seats) {
            seat.assignReservation(reservation);
            seat.assignTo(null);
            seat.assignPreemptionToken(null);
            seat.assignPreemptedAt(null);
            seat.assignPreemptedUntil(null);
            seat.setStatusTo(reservedStatus);
        }

        seatRepository.saveAll(seats);
    }

    private ReservedSeatInfo convertToReservedSeatInfo(Seat seat) {
        return ReservedSeatInfo.builder()
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatGrade(seat.getSeatGrade())
                .seatPrice(seat.getSeatPrice())
                .build();
    }
}
