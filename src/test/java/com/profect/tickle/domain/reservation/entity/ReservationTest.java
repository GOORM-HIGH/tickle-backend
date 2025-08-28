package com.profect.tickle.domain.reservation.entity;

import static com.profect.tickle.domain.reservation.entity.ReservationTestFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.profect.tickle.global.status.Status;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReservationTest {

    @DisplayName("예매에 좌석 할당 시 양방향 연관관계가 올바르게 설정된다")
    @Test
    public void assignSeatSetsBidirectionalRelationship() {
        //given
        Reservation reservation = createReservation();
        Seat seat = createSeat();

        //when
        reservation.assignSeat(seat);

        //then
        assertThat(reservation.getSeats()).contains(seat);
        assertThat(seat.getReservation()).isEqualTo(reservation);
    }

    @DisplayName("예매 취소 시 상태가 변경되고 업데이트 시간이 설정된다")
    @Test
    void cancelUpdatesStatusAndTimestamp() {
        // given
        Reservation reservation = createReservation();
        Status reservationCancelledStatus = mockReservationCancelledStatus(); // 예매 취소 상태
        Status seatAvailableStatus = mockSeatAvailableStatus(); // 좌석 선택 가능 상태
        Instant beforeCancel = Instant.now();

        // when
        reservation.cancel(reservationCancelledStatus, seatAvailableStatus);

        // then
        assertThat(reservation.getStatus()).isEqualTo(reservationCancelledStatus);
        assertThat(reservation.getUpdatedAt()).isNotNull();
        assertThat(reservation.getUpdatedAt()).isAfterOrEqualTo(beforeCancel);
    }

    @DisplayName("예매 취소 시 연관된 좌석들과의 관계가 모두 해제된다")
    @Test
    void cancelClearsAllSeatRelationships() {
        // given
        Reservation reservation = createReservationWithSeats(2);
        Status reservationCancelledStatus = mockReservationCancelledStatus();
        Status seatAvailableStatus = mockSeatAvailableStatus();

        // when
        reservation.cancel(reservationCancelledStatus, seatAvailableStatus);


        // then
        assertThat(reservation.getSeats()).isEmpty();
        // 좌석의 resetForCancellation이 호출되었는지는 실제 좌석 객체로 검증해야 함
    }
}
