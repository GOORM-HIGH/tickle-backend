package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.reservation.dto.response.preemption.PreemptedSeatInfo;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservationInfoResponseDto {
    private List<PreemptedSeatInfo> seats;         // 선점된 좌석 정보
    private Integer totalAmount;                   // 총 결제 금액
    private Integer currentPoints;                 // 보유 포인트
    private boolean sufficientPoints;              // 포인트 충분 여부
    private List<CouponResponseDto> coupons;     // 사용 가능한 쿠폰들
    private Instant preemptedUntil;                // 선점 만료 시간
}

