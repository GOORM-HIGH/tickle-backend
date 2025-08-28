package com.profect.tickle.domain.reservation.service;

import static org.mockito.Mockito.*;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.reservation.entity.Reservation;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.domain.reservation.entity.SeatGrade;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;

import java.time.Instant;

public class SeatPreemptionTestFixture {

    public static Seat createAvailableSeat(Long seatId, Long performanceId) {
        Performance performance = mockPerformance(performanceId);
        Status availableStatus = mockAvailableStatus();

        Seat seat = Seat.create(performance, SeatGrade.VIP, "A" + seatId, 50000, availableStatus, Instant.now());
        setPrivateField(seat, "id", seatId);
        return seat;
    }

    public static Seat createUnavailableSeat(Long seatId, Long performanceId) {
        Performance performance = mockPerformance(performanceId);
        Status reservedStatus = mockReservedStatus();

        Seat seat = Seat.create(performance, SeatGrade.VIP, "A" + seatId, 50000, reservedStatus, Instant.now());
        setPrivateField(seat, "id", seatId);

        // 이미 예매된 좌석으로 만들어서 선점 불가능하게 설정
        setPrivateField(seat, "reservation", mock(Reservation.class));
        return seat;
    }

    public static Seat createSeatFromDifferentPerformance(Long seatId, Long differentPerformanceId) {
        Performance performance = mockPerformance(differentPerformanceId);
        Status availableStatus = mockAvailableStatus();

        Seat seat = Seat.create(performance, SeatGrade.VIP, "A" + seatId, 50000, availableStatus, Instant.now());
        setPrivateField(seat, "id", seatId);
        return seat;
    }

    public static Performance mockPerformance(Long performanceId) {
        Performance performance = mock(Performance.class);
        when(performance.getId()).thenReturn(performanceId);
        return performance;
    }

    public static Status mockAvailableStatus() {
        Status status = mock(Status.class);
        lenient().when(status.getId()).thenReturn(StatusIds.Seat.AVAILABLE);
        return status;
    }

    public static Status mockReservedStatus() {
        Status status = mock(Status.class);
        lenient().when(status.getId()).thenReturn(StatusIds.Seat.RESERVED);
        return status;
    }

    public static Status mockPreemptedStatus() {
        Status status = mock(Status.class);
        lenient().when(status.getId()).thenReturn(StatusIds.Seat.PREEMPTED);
        return status;
    }

    public static Member mockMember() {
        return Member.builder()
                .id(1L)
                .build();
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}