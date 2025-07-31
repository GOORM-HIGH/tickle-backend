package com.profect.tickle.domain.member.entity;

import com.profect.tickle.domain.event.entity.Coupon;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coupon_received",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "coupon_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CouponReceived {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_received_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "coupon_received_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "coupon_received_updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    private CouponReceived(Member member, Coupon coupon) {
        this.member = member;
        this.coupon = coupon;
    }

    public static CouponReceived create(Member member, Coupon coupon) {
        return new CouponReceived (member, coupon);
    }
}