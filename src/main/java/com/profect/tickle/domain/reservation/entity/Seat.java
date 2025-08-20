package com.profect.tickle.domain.reservation.entity;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "seat_code", length = 30)
    private String seatCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", length = 3, nullable = false)
    private SeatGrade seatGrade; // "VIP", "R", "S"

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber; // "A1", "B2"

    @Column(name = "seat_price", nullable = false)
    private Integer seatPrice;

    @Column(name = "seat_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "preempted_at")
    private Instant preemptedAt;

    @Column(name = "preempted_until")
    private Instant preemptedUntil;

    @Column(name = "preemption_token")
    private String preemptionToken;

    public void assignEvent(Event event) {this.event = event;}

    void assignReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    // ==== 좌석 상태 관리 메서드들 ====

    public void preempt(String preemptionToken, Instant preemptedAt, Instant preemptedUntil, Member member, Status preemptedStatus) {
        this.preemptionToken = preemptionToken;
        this.preemptedAt = preemptedAt;
        this.preemptedUntil = preemptedUntil;
        this.member = member;
        this.status = preemptedStatus;
    }

    public void releasePreemption() {
        this.preemptionToken = null;
        this.preemptedAt = null;
        this.preemptedUntil = null;
        this.member = null;
    }

    public void completeReservation(Member member, Status reservedStatus, String seatCode) {
        this.member = member;
        this.status = reservedStatus;
        this.seatCode = seatCode;
        this.preemptionToken = null;
        this.preemptedAt = null;
        this.preemptedUntil = null;
    }

    /**
     * 예매 취소 시 좌석 상태 초기화
     * Reservation.cancel()에서 호출되는 내부 메서드
     */
    public void resetForCancellation(Status availableStatus) {
        this.status = availableStatus;
        this.member = null;
        this.seatCode = null;
        // reservation은 Reservation에서 처리하므로 건드리지 않음
    }

    public void setStatusTo(Status status){
        this.status = status;
    }
}
