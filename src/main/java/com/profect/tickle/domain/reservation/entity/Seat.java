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

    @Column(name = "seat_code", length = 15)
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

    public void assignTo(Member member) {
        this.member = member;
    }

    public void assignReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void assignPreemptionToken(String preemptionToken) {
        this.preemptionToken = preemptionToken;
    }

    public void assignPreemptedAt(Instant now) {
        this.preemptedAt = now;
    }

    public void assignPreemptedUntil(Instant preemptedUntil) {
        this.preemptedUntil = preemptedUntil;
    }

    public void setStatusTo(Status status){
        this.status = status;
    }

    public void assignSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }
}
