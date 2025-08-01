package com.profect.tickle.domain.point.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "Point")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long pointId;  // 포인트 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "point_credit", nullable = false)
    private Integer credit;  // 충전/차감 포인트 (양수: 충전, 음수: 차감)

    @Enumerated(EnumType.STRING)
    @Column(name = "point_target")
    private PointTarget target;  // 포인트 사용 대상 (예약, 이벤트)

    @Column(name = "point_result", nullable = false)
    private Integer result;  // 포인트 잔액 결과

    @Column(name = "point_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    private Point(Member member, Integer credit, PointTarget target, Integer result) {
        this.member = member;
        this.credit = credit;
        this.target = target;
        this.result = result;

    }

    public static Point create(Member member, int amount, PointTarget target, int result) {
        return new Point(member, amount, target, result);
    }
}
