package com.profect.tickle.domain.reservation.entity;

import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "reservation_code", length = 30, nullable = false)
    private String code;

    @Column(name = "reservation_price", nullable = false)
    private Integer price;

    @Column(name = "reservation_is_notify", nullable = false)
    private Boolean isNotify;

    @Column(name = "reservation_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reservation_updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    public static Reservation create(Member member, Performance performance, Status status, Integer price) {
        Reservation reservation = new Reservation();
        reservation.member = member;
        reservation.performance = performance;
        reservation.status = status;
        reservation.code = generateReservationCode();
        reservation.price = price;
        reservation.isNotify = true;
        reservation.createdAt = Instant.now();
        return reservation;
    }

    public void assignSeat(Seat seat) {
        this.seats.add(seat);
        seat.assignReservation(this); // 연관관계 편의 메서드
    }

    public void changeStatusTo(Status status) {
        this.status = status;
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    private static String generateReservationCode() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuidPart + dateTime;
    }
}
