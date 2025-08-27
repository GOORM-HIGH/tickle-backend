package com.profect.tickle.domain.reservation.entity;

import static org.mockito.Mockito.*;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SeatTestFixture {

    // Mock 객체 생성 메서드들
    public static Performance mockPerformance() {
        Performance performance = mock(Performance.class);
        when(performance.getId()).thenReturn(1L);
        return performance;
    }

    public static Status mockAvailableStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Seat.AVAILABLE);
        return status;
    }

    public static Status mockPreemptedStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Seat.PREEMPTED);
        return status;
    }

    public static Status mockReservedStatus() {
        Status status = mock(Status.class);
        when(status.getId()).thenReturn(StatusIds.Seat.RESERVED);
        return status;
    }

    public static Member mockMember() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        return member;
    }

    public static Reservation mockReservation() {
        return mock(Reservation.class);
    }

    // 기본 좌석 생성
    public static Seat createSeat() {
        return Seat.create(
                mockPerformance(),
                SeatGrade.VIP,
                "A1",
                50000,
                mockAvailableStatus(),
                Instant.now()
        );
    }


    // 선점 설정 헬퍼
    public static void setActivePreemption(Seat seat) {
        seat.preempt(
                "token123",
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.MINUTES),
                mockMember(),
                mockPreemptedStatus()
        );
    }

    // 예약 설정 헬퍼
    public static void setReservation(Seat seat) {
        setPrivateField(seat, "reservation", mockReservation());
    }

    public static void setReservation(Seat seat, Reservation reservation) {
        setPrivateField(seat, "reservation", reservation);
    }

    // 리플렉션 유틸
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