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

    // 예매가 삭제 되면 좌석도 삭제 되는가? 지금의 설정에 대해 공부해보자.
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

    // 연관관계 편의 메서드 - 양방향 설정
    public void assignSeat(Seat seat) {
        this.seats.add(seat);
        seat.assignReservation(this);
    }

    /**
     * 예매 취소 - 좌석들의 상태까지 함께 관리
     * 예매가 주도하여 관련된 모든 상태를 변경
     */
    public void cancel(Status reservationCancledStatus, Status seatAvailableStatus) {
        // 1. 예매 자체 상태 변경
        this.status = reservationCancledStatus;
        this.updatedAt = Instant.now();

        // 2. 연관된 좌석들도 함께 처리 (예매가 주도)
        for (Seat seat : new ArrayList<>(this.seats)) {
            // 좌석 상태 초기화 (예매가 주도하여 처리)
            seat.resetForCancellation(seatAvailableStatus);

            // 연관관계 해제 (양방향)
            this.seats.remove(seat);
            seat.assignReservation(null);
        }
    }

    private static String generateReservationCode() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return uuidPart + dateTime;
    }
}
