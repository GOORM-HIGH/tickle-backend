package com.profect.tickle.global.status;

import com.profect.tickle.domain.event.dto.request.TicketEventCreateRequestDto;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.reservation.entity.Seat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;


@Getter
@Entity
@Table(name = "status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;

    @Column(name = "status_type", length = 5, nullable = false)
    private String type; // 도메인별 상태 구분

    @Column(name = "status_code", nullable = false)
    private Short code; // 도메인별 상태 코드

    @Column(name = "status_description", length = 20, nullable = false)
    private String description;

    @Column(name = "status_created_at", nullable = false)
    private Instant createdAt;

    private Status(String type, Short code, String name) {
        this.type = type;
        this.code = code;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public static Status create(String type, Short code, String name) {
        return new Status(
                type,
                code,
                name);
    }

    public void scheduled() {
        this.code = 100;
    }

    public void process() {
        this.code = 101;
    }

    public void complete() {
        this.code = 102;
    }

    public void cancel() {
        this.code = 103;
    }
}

