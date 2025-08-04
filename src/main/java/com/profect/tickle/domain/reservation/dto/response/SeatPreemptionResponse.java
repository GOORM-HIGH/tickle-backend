package com.profect.tickle.domain.reservation.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SeatPreemptionResponse {
    private boolean success;                 // 성공 여부
    private String preemptionToken;          // 선점 토큰 (성공시에만)
    private Instant preemptedUntil;          // 선점 만료 시간 (성공시에만)
    private List<PreemptedSeatInfo> seats;   // 선점된 좌석 정보 (성공시에만)
    private String message;                  // 결과 메시지
    private List<Long> unavailableSeatIds;   // 선점 불가능한 좌석 ID들 (실패시에만)

    // 성공 응답 생성
    public static SeatPreemptionResponse success(String preemptionToken, Instant preemptedUntil,
            List<PreemptedSeatInfo> seats, String message) {
        return SeatPreemptionResponse.builder()
                .success(true)
                .preemptionToken(preemptionToken)
                .preemptedUntil(preemptedUntil)
                .seats(seats)
                .message(message)
                .build();
    }

    // 실패 응답 생성
    public static SeatPreemptionResponse failure(String message, List<Long> unavailableSeatIds) {
        return SeatPreemptionResponse.builder()
                .success(false)
                .message(message)
                .unavailableSeatIds(unavailableSeatIds)
                .build();
    }
}