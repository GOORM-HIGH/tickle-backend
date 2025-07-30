package com.profect.tickle.domain.reservation.entity;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "seat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_class_id", nullable = false)
    private SeatClass seatClass;

    @Column(name = "seat_code", length = 15)
    private String seatCode;

    @Column(name = "seat_number", nullable = false)
    private Short seatNumber;

    @Column(name = "seat_created_at", nullable = false)
    private LocalDateTime createdAt;

}
