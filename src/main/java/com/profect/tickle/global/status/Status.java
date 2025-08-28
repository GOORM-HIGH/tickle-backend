package com.profect.tickle.global.status;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@Entity
@Table(name = "status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;

    @Column(name = "status_type", length = 20, nullable = false)
    private String type; // 도메인별 상태 구분

    @Column(name = "status_code", nullable = false)
    private Short code; // 도메인별 상태 코드

    @Column(name = "status_description", length = 20, nullable = false)
    private String description;

    @Column(name = "status_created_at", nullable = false)
    private Instant createdAt;

    private Status(Long id, String type, Short code, String description, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.code = code;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static Status create(Long id,  String type, Short code, String description, Instant createdAt) {
        return new Status(id, type, code, description, createdAt);
    }

}

