package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.dto.response.reservation.PaymentInfo;
import com.profect.tickle.domain.reservation.dto.response.reservation.PerformanceInfo;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationCancelResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDetailResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationHistoryResponseDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationInfo;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservationHistoryService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;
    private final StatusProvider statusProvider;

    public List<ReservationHistoryResponseDto> getReservationHistoryWithStatus(Long userId, Long statusId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByMemberIdOrderByCreatedAtDesc(userId, pageable);

        log.info("reservations count: {}", reservations.getTotalElements());

        return reservations.getContent().stream()
                .filter(reservation -> Objects.equals(reservation.getStatus().getId(), statusId))
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    public ReservationDetailResponseDto getReservationDetail(Long reservationId, Long userId) {
        Reservation reservation = getReservation(reservationId, userId);

        // 좌석 정보 조회
        List<Seat> seats = seatRepository.findByReservationId(reservationId);

        return convertToDetailResponse(reservation, seats);
    }


    @Transactional
    public ReservationCancelResponseDto cancelReservation(Long reservationId) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            Reservation reservation = getReservation(reservationId, userId);

            // 취소 가능 여부 확인
            if (!isCancellable(reservation)) {
                return ReservationCancelResponseDto.failure("취소할 수 없는 예매입니다.");
            }

            // 좌석 상태 변경 (예매완료 → 예매가능)
            List<Seat> seats = seatRepository.findByReservationId(reservationId);
            updateSeatsToAvailable(seats);

            // 예매 상태 변경
            Status canceled = statusProvider.provide(StatusIds.Reservation.CANCELLED);

            reservation.changeStatusTo(canceled);
            reservation.markUpdated();

            reservationRepository.save(reservation);

            // 포인트 환불
            Integer refundAmount = reservation.getPrice();

            Member member = memberRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Point point = member.refundPoint(refundAmount, PointTarget.REFUND);
            pointRepository.save(point);

            member.addPoint(refundAmount);

            return ReservationCancelResponseDto.success(refundAmount);

        } catch (BusinessException e) {
            return ReservationCancelResponseDto.failure(e.getMessage());
        }
    }

    private ReservationHistoryResponseDto convertToHistoryResponse(Reservation reservation) {
        List<Seat> seats = seatRepository.findByReservationId(reservation.getId());

        List<String> seatNumbers = seats.stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());

        return ReservationHistoryResponseDto.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getCode())
                .performanceTitle(reservation.getPerformance().getTitle())
                .performanceHall(reservation.getPerformance().getHall().getAddress())
                .performanceDate(reservation.getPerformance().getDate())
                .seatCount(seats.size())
                .seatNumbers(seatNumbers)
                .price(reservation.getPrice())
                .status(reservation.getStatus().getDescription())
                .reservedAt(reservation.getCreatedAt())
                .cancellable(isCancellable(reservation))
                .build();
    }

    private ReservationDetailResponseDto convertToDetailResponse(Reservation reservation, List<Seat> seats) {

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
        List<ReservedSeatDto> seatDetails = seats.stream()
                .map(seat -> ReservedSeatDto.builder()
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

        return ReservationDetailResponseDto.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getCode())
                .performance(performanceInfo)
                .seats(seatDetails)
                .payment(paymentInfo)
                .reservation(reservationInfo)
                .build();
    }

    private boolean isCancellable(Reservation reservation) {
        if (!Objects.equals(reservation.getStatus().getId(), StatusIds.Reservation.PAID)) {
            return false;
        }

        // 공연 예매 종료일까지만 취소 가능
        Instant performanceStartDate = reservation.getPerformance().getEndDate();
        Instant today = Instant.now();

        return today.isBefore(performanceStartDate);
    }

    private void updateSeatsToAvailable(List<Seat> seats) {
        Status availableStatus = statusProvider.provide(StatusIds.Seat.AVAILABLE);

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
