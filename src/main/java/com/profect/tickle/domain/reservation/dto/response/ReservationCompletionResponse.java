package com.profect.tickle.domain.reservation.dto.response;

import com.profect.tickle.domain.reservation.entity.Reservation;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReservationCompletionResponse {
    private boolean success;
    private Long reservationId;                    // 생성된 예매 ID
    private String reservationNumber;              // 예매 번호
    private List<ReservedSeatInfo> reservedSeats;  // 예매된 좌석 정보
    private Integer price;                         // 결제 포인트
    private Integer remainingPoints;               // 결제 후 잔여 포인트
    private Instant reservedAt;                    // 예매 완료 시간
    private String message;

    public static ReservationCompletionResponse success(Reservation reservation,
            List<ReservedSeatInfo> seats,
            Integer remainingPoints) {
        return ReservationCompletionResponse.builder()
                .success(true)
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getCode())
                .reservedSeats(seats)
                .price(reservation.getPrice())
                .remainingPoints(remainingPoints)
                .reservedAt(reservation.getCreatedAt())
                .message("예매가 완료되었습니다.")
                .build();
    }

    public static ReservationCompletionResponse failure(String message) {
        return ReservationCompletionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}