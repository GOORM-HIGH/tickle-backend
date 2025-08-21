package com.profect.tickle.domain.reservation.service;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.service.CouponService;
import com.profect.tickle.domain.point.service.PointService;
import com.profect.tickle.domain.reservation.dto.response.preemption.PreemptedSeatInfo;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationInfoResponseDto;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationInfoService {

    private final SeatRepository seatRepository;
    private final PointService pointService;
    private final CouponService couponService;

    public ReservationInfoResponseDto getReservationInfo(String preemptionToken) {
        Long memberId = SecurityUtil.getSignInMemberId();

        // 1. 선점된 좌석들 조회
        List<Seat> seats = seatRepository.findByPreemptionToken(preemptionToken);

        // 2.선점 토큰 유효성 검증
        validatePreemptionToken(seats, memberId);

        // 3. 총 결제 금액 계산
        int totalAmount = calculateTotalAmount(seats);

        // 4. 사용자 보유 포인트 조회
        int currentPoints = pointService.getCurrentPoint().credit();

        // 5. 사용 가능한 쿠폰들 조회
        List<CouponResponseDto> availableCoupons = couponService.getAvailableCoupons(memberId);

        // 6. 좌석 정보 변환
        List<PreemptedSeatInfo> seatInfos = seats.stream()
                .map(this::convertToPreemptedSeatInfo)
                .collect(Collectors.toList());

        return ReservationInfoResponseDto.builder()
                .seats(seatInfos)
                .totalAmount(totalAmount)
                .currentPoints(currentPoints)
                .sufficientPoints(currentPoints >= totalAmount)
                .coupons(availableCoupons)
                .preemptedUntil(seats.getFirst().getPreemptedUntil())
                .build();
    }

    private void validatePreemptionToken(List<Seat> seats, Long userId) {
        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.PREEMPTION_TOKEN_INVALID);
        }

        if (!seats.getFirst().isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PREEMPTION_TOKEN_INVALID);
        }
    }

    private int calculateTotalAmount(List<Seat> seats) {
        return seats.stream()
                .mapToInt(Seat::getSeatPrice)
                .sum();
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
