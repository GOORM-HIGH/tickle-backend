package com.profect.tickle.domain.event.entity;

import com.profect.tickle.domain.member.entity.CouponReceived;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @OneToOne(mappedBy = "coupon", fetch = FetchType.LAZY)
    private Event event;

    @Column(name = "coupon_name", length = 10, nullable = false)
    private String name;

    @Column(name = "coupon_content", length = 20, nullable = false)
    private String content;

    @Column(name = "coupon_count", nullable = false)
    private Short count;

    @Column(name = "coupon_rate", nullable = false)
    private Short rate;

    @Column(name = "coupon_valid", nullable = false)
    private Instant valid;

    @Column(name = "coupon_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "coupon_updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponReceived> receivedCoupons = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    private Coupon(String name, String content, Short count, Short rate, Instant valid) {
        this.name = name;
        this.content = content;
        this.count = count;
        this.rate = rate;
        this.valid = valid;
    }

    public static Coupon create(String name, String content, Short count, Short rate, Instant valid) {
        return new Coupon(name, content, count, rate, valid);
    }

    public void updateEvent(Event event) {
        this.event = event;
    }

    public void decreaseCount() {
        if (this.count <= 0) {throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);}
        this.count--;
    }
}
