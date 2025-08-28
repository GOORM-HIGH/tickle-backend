package com.profect.tickle.domain.reservation.entity;

import static com.profect.tickle.domain.reservation.entity.SeatTestFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.status.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeatTest {

    @DisplayName("생성 후 초기 상태의 좌석은 선점할 수 있다")
    @Test
    public void initialSeatCanBePreempted() {
        //given
        Seat seat = createSeat();

        //when
        boolean result = seat.isAvailableForPreemption();

        //then
        assertThat(result).isTrue();
    }

    @DisplayName("이미 예매된 좌석은 선점할 수 없다")
    @Test
    void reservedSeatCannotBePreempted() {
        // given
        Seat seat = createSeat();

        setReservation(seat);
        seat.completeReservation(mockMember(), mockReservedStatus(), "SEAT001");

        // when
        boolean result = seat.isAvailableForPreemption();

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("다른 고객이 선점 중인 좌석은 선점할 수 없다")
    @Test
    void preemptedSeatCannotBePreempted() {
        // given
        Seat seat = createSeat();
        setActivePreemption(seat);

        // when
        boolean result = seat.isAvailableForPreemption();

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("예매 완료 시 선점 정보가 모두 초기화된다")
    @Test
    void completeReservationClearsPreemptionData() {
        // given
        Seat seat = createSeat();
        setActivePreemption(seat); // 먼저 선점 상태로 만들고

        Member member = mockMember();
        Status reservedStatus = mockReservedStatus();
        String seatCode = "SEAT001";

        // when
        seat.completeReservation(member, reservedStatus, seatCode);

        // then
        assertThat(seat.getPreemptionToken()).isNull();
        assertThat(seat.getPreemptedAt()).isNull();
        assertThat(seat.getPreemptedUntil()).isNull();
        assertThat(seat.getMember()).isEqualTo(member);
        assertThat(seat.getStatus()).isEqualTo(reservedStatus);
        assertThat(seat.getSeatCode()).isEqualTo(seatCode);
    }

    @DisplayName("선점 해제 시 모든 선점 관련 필드가 초기화되고 선점 할 수 있게 된다.")
    @Test
    void releasePreemptionClearsAllPreemptionFields() {
        // given
        Seat seat = createSeat();
        setActivePreemption(seat);

        Status availableStatus = mockAvailableStatus();

        // when
        seat.releasePreemption(availableStatus);

        // then
        assertThat(seat.getPreemptionToken()).isNull();
        assertThat(seat.getPreemptedAt()).isNull();
        assertThat(seat.getPreemptedUntil()).isNull();
        assertThat(seat.getMember()).isNull();

        boolean result = seat.isAvailableForPreemption();
        assertThat(result).isTrue();
    }

    @DisplayName("예매 취소 시 좌석 상태가 초기화되고 reservation은 유지된다")
    @Test
    void resetForCancellationClearsStateButKeepsReservation() {
        // given
        Seat seat = createSeat();
        Member member = mockMember();
        Status reservedStatus = mockReservedStatus();
        Status availableStatus = mockAvailableStatus();
        Reservation reservation = mockReservation();

        // 예매 완료 상태로 설정
        setReservation(seat, reservation);
        seat.completeReservation(member, reservedStatus, "SEAT001");

        // when
        seat.resetForCancellation(availableStatus);

        // then - 초기화되는 필드들
        assertThat(seat.getStatus()).isEqualTo(availableStatus);
        assertThat(seat.getMember()).isNull();
        assertThat(seat.getSeatCode()).isNull();

        // then - 유지되는 필드들 (reservation은 Reservation에서 처리)
        assertThat(seat.getReservation()).isEqualTo(reservation); // 유지됨
        assertThat(seat.getPreemptionToken()).isNull(); // 이미 null이었음
    }
}