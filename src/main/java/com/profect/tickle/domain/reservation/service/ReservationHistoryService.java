package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.domain.point.repository.PointRepository;
import com.profect.tickle.domain.reservation.dto.response.ReservationCancelResponse;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.ReservationStatus;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.entity.SeatStatus;
import com.profect.tickle.domain.reservation.repository.ReservationRepository;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public ReservationCancelResponse cancelReservation(Long reservationId) {

        Long userId = SecurityUtil.getSignInMemberId();

        try {
            Reservation reservation = reservationRepository.findByIdAndMemberId(reservationId,
                            userId)
                    .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다."));

            // 취소 가능 여부 확인
            if (!isCancellable(reservation)) {
                return ReservationCancelResponse.failure("취소할 수 없는 예매입니다.");
            }

            // 좌석 상태 변경 (예매완료 → 예매가능)
            List<Seat> seats = seatRepository.findByReservationId(reservationId);
            updateSeatsToAvailable(seats);

            // 예매 상태 변경
            Status canceled = statusRepository.findById(ReservationStatus.CANCELED.getId())
                    .orElseThrow();

            reservation.changeStatusTo(canceled);

            reservationRepository.save(reservation);

            // 포인트 환불
            Integer refundAmount = reservation.getPrice();

            Member member = memberRepository.findById(userId)
                    .orElseThrow();

            Point point = Point.refund(member, refundAmount, PointTarget.REFUND);
            pointRepository.save(point);

            member.addPoint(refundAmount);

            return ReservationCancelResponse.success(refundAmount);

        } catch (Exception e) {
            return ReservationCancelResponse.failure(e.getMessage());
        }
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
                .orElseThrow(() -> new IllegalStateException("예매가능 상태를 찾을 수 없습니다."));

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
}
