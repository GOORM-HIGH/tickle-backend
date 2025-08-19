package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.domain.member.entity.Member;
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
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDto;
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
    private final ReservationValidator reservationValidator;

    @Transactional
    public ReservationCompletionResponseDto completeReservation(
            ReservationCompletionRequestDto request) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            // 1. 선점 토큰으로 좌석들 조회
            List<Seat> preemptedSeats = seatRepository.findByPreemptionTokenWithLock(request.getPreemptionToken());

            // 좌석들 검증
            reservationValidator.validatePreemptedSeats(preemptedSeats, userId);

            // 2. 쿠폰 할인 계산
            int finalAmount = calculateFinalAmount(request, userId);

            // 3. 최종 결제 금액 검증 - 요청의 최종 결제 금액과 현재 로직에서 계산한 값이 일치하는지 검증한다.
            reservationValidator.validatePaymentAmount(finalAmount, request.getTotalAmount());

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
            publishReservationSuccessEvent(reservation.getId(), userId);

            return ReservationCompletionResponseDto.success(reservation, reservedSeats, remainingPoints);

        } catch (BusinessException e) {
            log.error("Reservation completion failed", e);
            return ReservationCompletionResponseDto.failure(e.getMessage());
        }
    }

    private int calculateFinalAmount(ReservationCompletionRequestDto request, Long userId) {

        // 요청에서 넘어온 쿠폰 조회
        Long couponId = request.getCouponId();

        if (couponId == null) {
            return request.getTotalAmount();
        }
        // 쿠폰이 존재 할 경우, 할인 금액을 계산한다.
        CouponReceived couponReceived = couponService.findValidCoupon(couponId, userId);
        int discountAmount = couponReceived.calculateDiscountAmount(request.getTotalAmount());

        // 최종 결제 금액을 반환한다.
        return request.getTotalAmount() - discountAmount;
    }

    // 예매 성공 이벤트 발행 메서드
    private void publishReservationSuccessEvent(long reservationId, long userId) {
        PerformanceDto reservedPerformance = performanceMapper.findByReservationId(reservationId); // 예매 공연 정보
        log.info("예약된 공연 정보: {}", reservedPerformance.getTitle());
        ReservationDto reservation = reservationMapper.findById(reservationId).orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND)); // 예매 정보
        log.info("예약ID로 찾은 예약 정보: {}", reservation.getId());
        Member siginMember = memberRepository.findById(userId) // 예매 유저 정보
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage(),
                        ErrorCode.MEMBER_NOT_FOUND)
                );
        eventPublisher.publishEvent(new ReservationSuccessEvent(reservedPerformance, reservation, siginMember));
        log.info("예매 성공 이벤트 발행 완료");
    }

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
        return reservationMapper.findReservedSeatById(reservationId);
    }

    private String generateSeatCode() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuidPart + dateTime;
    }
}
