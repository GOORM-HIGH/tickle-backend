package com.profect.tickle.domain.event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(name = "coupon_name", length = 10, nullable = false)
    private String name;

    @Column(name = "coupon_count", nullable = false)
    private Short count;

    @Column(name = "coupon_rate", nullable = false)
    private Short rate;

    @Column(name = "coupon_valid", nullable = false)
    private LocalDate valid;

    @Column(name = "coupon_created_at", nullable = false)
    private LocalDateTime createdAt;
}
