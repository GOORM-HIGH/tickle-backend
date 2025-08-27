package com.profect.tickle.domain.event.entity;

import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @OneToOne(mappedBy = "event")
    private Seat seat;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", unique = true)
    private Coupon coupon;

    @Column(name = "event_name", length = 20, nullable = false)
    private String name;

    @Column(name = "event_goal_price")
    private Integer goalPrice;

    @Column(name = "event_accrued")
    private Integer accrued;

    @Column(name = "event_per_price")
    private Short perPrice;

    @Column(name = "event_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "event_updated_at", nullable = false)
    private Instant updatedAt;

    @Convert(converter = EventTypeConverter.class)
    @Column(name = "event_type", nullable = false)
    private EventType type;

    private Event(Status status, Seat seat, String name, Integer goalPrice, Short perPrice) {
        this.status = status;
        this.coupon = null;
        this.seat = seat;
        this.name = name;
        this.goalPrice = goalPrice;
        this.perPrice = perPrice;
        this.accrued = 0;
        this.type = EventType.TICKET;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private Event(Status status, Coupon coupon, String name) {
        this.status = status;
        this.coupon = coupon;
        this.seat = null;
        this.name = name;
        this.goalPrice = null;
        this.perPrice = null;
        this.accrued = null;
        this.type = EventType.COUPON;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Event create(Status status, Seat seat, TicketEventCreateRequestDto dto) {
        return new Event(
                status,
                seat,
                dto.name(),
                dto.goalPrice(),
                dto.perPrice()
        );
    }

    public static Event create(Status status, Coupon coupon, String name) {
        return new Event(
                status,
                coupon,
                name
        );
    }

    public void accumulate(Short perPrice) {
        if (!this.status.getId().equals(StatusIds.Event.IN_PROGRESS)) {
            throw  new BusinessException(ErrorCode.EVENT_NOT_IN_PROGRESS);
        }
        this.accrued += perPrice;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }
}
