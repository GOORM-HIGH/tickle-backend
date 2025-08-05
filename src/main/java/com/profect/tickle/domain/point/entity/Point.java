package com.profect.tickle.domain.point.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "point")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    @Id
    @Column(name = "point_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "point_credit", nullable = false)
    private int credit;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_target", nullable = false)
    private PointTarget target;

    @Column(name = "point_order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "point_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Point(Member member, int credit, PointTarget target, String orderId) {
        this.member = member;
        this.credit = credit;
        this.target = target;
        this.orderId = orderId;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static Point charge(Member member, int amount, String orderId) {
        return new Point(member, amount, PointTarget.CHARGE, orderId);
    }

    public static Point deduct(Member member, int amount, PointTarget target) {
        return new Point(member, -amount, target, generateInternalOrderId());
    }

    public static Point refund(Member member, int amount, PointTarget target) {
        return new Point(member, amount, target, generateInternalOrderId());
    }

    private static String generateInternalOrderId() {
        return "deduct_" + System.currentTimeMillis();
    }
}
