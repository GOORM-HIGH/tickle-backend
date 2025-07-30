package com.profect.tickle.domain.event.entity;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
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

    private Coupon(String name, Short count, Short rate, LocalDate valid, LocalDateTime createdAt) {
        this.name = name;
        this.count = count;
        this.rate = rate;
        this.valid = valid;
        this.createdAt = createdAt;
    }

    public static Coupon create(String name, Short count, Short rate, LocalDate valid) {
        if (count < 0 || rate < 0) {
            throw new BusinessException(ErrorCode.INVALID_COUPON_VALUE);
        }
        if (valid.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_DATE);
        }
        return new Coupon(name, count, rate, valid, LocalDateTime.now());
    }
}
