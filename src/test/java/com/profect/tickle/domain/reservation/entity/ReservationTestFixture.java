package com.profect.tickle.domain.reservation.entity;

import static org.mockito.Mockito.*;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;

public class ReservationTestFixture {

    public static Member mockMember() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        return member;
    }

    public static Performance mockPerformance() {
        Performance performance = mock(Performance.class);
        when(performance.getId()).thenReturn(1L);
        return performance;
    }

    public static Status mockReservationPaidStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Reservation.PAID);
        return status;
    }

    public static Status mockReservationCancelledStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Reservation.CANCELLED);
        return status;
    }

    public static Status mockSeatAvailableStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Seat.AVAILABLE);
        return status;
    }

    public static Seat createSeat() {
        return SeatTestFixture.createSeat();
    }

    public static Reservation createReservation() {
        return Reservation.create(
                mockMember(),
                mockPerformance(),
                mockReservationPaidStatus(),
                50000
        );
    }

    public static Reservation createReservationWithSeats(int seatCount) {
        Reservation reservation = createReservation();

        for (int i = 0; i < seatCount; i++) {
            Seat seat = createSeat();
            reservation.assignSeat(seat);
        }

        return reservation;
    }
}