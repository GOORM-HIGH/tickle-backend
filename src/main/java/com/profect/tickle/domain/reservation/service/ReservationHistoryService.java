package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.dto.response.PaymentInfo;
import com.profect.tickle.domain.reservation.dto.response.PerformanceInfo;
import com.profect.tickle.domain.reservation.dto.response.ReservationCancelResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationDetailResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationHistoryResponse;
import com.profect.tickle.domain.reservation.dto.response.ReservationInfo;
import com.profect.tickle.domain.reservation.dto.response.ReservedSeatDetail;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationHistoryService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final StatusRepository statusRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;

    public List<ReservationHistoryResponse> getReservationHistory(Long userId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByMemberIdOrderByCreatedAtDesc(userId, pageable);

        return reservations.getContent().stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    public ReservationDetailResponse getReservationDetail(Long reservationId, Long userId) {
        Reservation reservation = getReservation(reservationId, userId);

        // 좌석 정보 조회
        List<Seat> seats = seatRepository.findByReservationId(reservationId);

        return convertToDetailResponse(reservation, seats);
    }


    @Transactional
    public ReservationCancelResponse cancelReservation(Long reservationId) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            Reservation reservation = getReservation(reservationId, userId);

            // 취소 가능 여부 확인
            if (!isCancellable(reservation)) {
                return ReservationCancelResponse.failure("취소할 수 없는 예매입니다.");
            }

            // 좌석 상태 변경 (예매완료 → 예매가능)
            List<Seat> seats = seatRepository.findByReservationId(reservationId);
            updateSeatsToAvailable(seats);

            // 예매 상태 변경
            Status canceled = statusRepository.findById(ReservationStatus.CANCELED.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

            reservation.changeStatusTo(canceled);

            reservationRepository.save(reservation);

            // 포인트 환불
            Integer refundAmount = reservation.getPrice();

            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Point point = Point.refund(member, refundAmount, PointTarget.REFUND);
            pointRepository.save(point);

            member.addPoint(refundAmount);

            return ReservationCancelResponse.success(refundAmount);

        } catch (BusinessException e) {
            return ReservationCancelResponse.failure(e.getMessage());
        }
    }

    private ReservationHistoryResponse convertToHistoryResponse(Reservation reservation) {
        List<Seat> seats = seatRepository.findByReservationId(reservation.getId());

        List<String> seatNumbers = seats.stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());

        return ReservationHistoryResponse.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getCode())
                .performanceTitle(reservation.getPerformance().getTitle())
                .performanceHall(reservation.getPerformance().getHall().getAddress())
                .performanceDate(reservation.getPerformance().getDate())
                .seatCount(seats.size())
                .seatNumbers(seatNumbers)
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .reservedAt(reservation.getCreatedAt())
                .cancellable(isCancellable(reservation))
                .build();
    }

    private ReservationDetailResponse  convertToDetailResponse(Reservation reservation, List<Seat> seats) {

        // 공연 정보
        Performance performance = reservation.getPerformance();
        PerformanceInfo performanceInfo = PerformanceInfo.builder()
                .performanceId(performance.getId())
                .title(performance.getTitle())
                .hall(performance.getHall().getAddress())
                .performanceDate(performance.getDate())
                .posterUrl(performance.getImg())
                .runtime(performance.getRuntime())
                .build();

        // 좌석 정보
        List<ReservedSeatDetail> seatDetails = seats.stream()
                .map(seat -> ReservedSeatDetail.builder()
                        .seatId(seat.getId())
                        .seatNumber(seat.getSeatNumber())
                        .seatGrade(seat.getSeatGrade())
                        .seatPrice(seat.getSeatPrice())
                        .build())
                .collect(Collectors.toList());

        // 결제 정보
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .price(reservation.getPrice())
                .paidAt(reservation.getCreatedAt())
                .build();

        // 예매 정보
        ReservationInfo reservationInfo = ReservationInfo.builder()
                .status(reservation.getStatus())
                .reservedAt(reservation.getCreatedAt())
                .cancelledAt(reservation.getUpdatedAt())
                .cancellable(isCancellable(reservation))
                .build();

        return ReservationDetailResponse.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getCode())
                .performance(performanceInfo)
                .seats(seatDetails)
                .payment(paymentInfo)
                .reservation(reservationInfo)
                .build();
    }

    private boolean isCancellable(Reservation reservation) {
        if (!Objects.equals(reservation.getStatus().getId(), ReservationStatus.PAID.getId())) {
            return false;
        }

        // 공연 시작일까지만 취소 가능
        Instant performanceStartDate = reservation.getPerformance().getStartDate();
        Instant today = Instant.now();

        return today.isBefore(performanceStartDate);
    }

    private void updateSeatsToAvailable(List<Seat> seats) {
        Status availableStatus = statusRepository.findById(SeatStatus.AVAILABLE.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        for (Seat seat : seats) {
            cancelSeatsOfReservation(seat, availableStatus);
        }

        seatRepository.saveAll(seats);
    }

    private void cancelSeatsOfReservation(Seat seat, Status availableStatus) {
        seat.assignReservation(null);
        seat.assignTo(null);
        seat.setStatusTo(availableStatus);
    }

    private Reservation getReservation(Long reservationId, Long userId) {
        return reservationRepository.findByIdAndMemberId(reservationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }
}
