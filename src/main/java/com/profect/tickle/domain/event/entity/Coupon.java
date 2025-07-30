package com.profect.tickle.domain.event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

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
    private Instant createdAt;

    private Coupon(String name, Short count, Short rate, LocalDate valid, Instant createdAt) {
        this.name = name;
        this.count = count;
        this.rate = rate;
        this.valid = valid;
        this.createdAt = createdAt;
    }

    public static Coupon create(String name, Short count, Short rate, LocalDate valid) {
        return new Coupon(name, count, rate, valid, Instant.now());
    }
}
