package com.profect.tickle.domain.event.entity;

import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "event_name", length = 20, nullable = false)
    private String name;

    @Column(name = "event_goal_price")
    private Integer goalPrice;

    @Column(name = "event_accrued")
    private Integer accrued;

    @Column(name = "event_per_price")
    private Short perPrice;

    @Column(name = "event_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "event_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = EventTypeConverter.class)
    @Column(name = "event_type", nullable = false)
    private EventType type;
}
