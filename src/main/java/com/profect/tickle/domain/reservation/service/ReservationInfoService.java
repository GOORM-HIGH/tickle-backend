package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.domain.reservation.dto.response.PreemptedSeatInfo;
import com.profect.tickle.domain.reservation.dto.response.ReservationInfoResponse;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.security.util.SecurityUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationInfoService {

    private final SeatRepository seatRepository;
    private final PointService pointService;
    private final CouponService couponService;

    public ReservationInfoResponse getReservationInfo(String preemptionToken) {
        // 1. 선점된 좌석들 조회
        List<Seat> seats = seatRepository.findByPreemptionToken(preemptionToken);

        Long userId = SecurityUtil.getSignInMemberId();

        if (seats.isEmpty() || !userId.equals(seats.get(0).getPreemptUserId())) {
            throw new IllegalArgumentException("유효하지 않은 선점 토큰입니다.");
        }

        // 2. 총 결제 금액 계산
        Integer totalAmount = seats.stream()
                .mapToInt(Seat::getSeatPrice)
                .sum();

        // 3. 사용자 보유 포인트 조회
        int currentPoints = pointService.getCurrentPoint().credit();

        // 4. 사용 가능한 쿠폰들 조회
        List<CouponResponseDto> availableCoupons = couponService.getAvailableCoupons(userId,
                totalAmount);

        // 5. 좌석 정보 변환
        List<PreemptedSeatInfo> seatInfos = seats.stream()
                .map(this::convertToPreemptedSeatInfo)
                .collect(Collectors.toList());

        return ReservationInfoResponse.builder()
                .seats(seatInfos)
                .totalAmount(totalAmount)
                .currentPoints(currentPoints)
                .sufficientPoints(currentPoints >= totalAmount)
                .coupons(availableCoupons)
                .preemptedUntil(seats.get(0).getPreemptedUntil())
                .build();
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
