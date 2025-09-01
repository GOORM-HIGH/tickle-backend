package com.profect.tickle.domain.point.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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
    @Column(name = "point_target", nullable = false, length = 30)
    private PointTarget target;

    @Column(name = "point_order_id", length = 100)
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

    public static Point charge(Member m, int amount, String externalOrderId) {
        String oid = (externalOrderId != null && !externalOrderId.isBlank())
                ? externalOrderId
                : generateInternalOrderId("charge");
        return new Point(m, amount, PointTarget.CHARGE, oid);
    }

    public static Point deduct(Member member, int amount, PointTarget target) {
        return new Point(member, -amount, target, generateInternalOrderId("deduct"));
    }

    public static Point refund(Member member, int amount, PointTarget target) {
        return new Point(member, amount, target, generateInternalOrderId("refund"));
    }

    private static String generateInternalOrderId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
    }
}
