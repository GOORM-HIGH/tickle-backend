package com.profect.tickle.global.status;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "status_name", length = 15, nullable = false)
    private String name;

    @Column(name = "status_created_at", nullable = false)
    private LocalDateTime createdAt;
}
