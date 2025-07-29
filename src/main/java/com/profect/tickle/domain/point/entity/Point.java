package com.profect.tickle.domain.point.entity;

import com.profect.tickle.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    @Column(name = "user_id", nullable = false)
    private User userId;  // 사용자 고유번호

    @Column(name = "point_credit", nullable = false)
    private Integer pointCredit;  // 충전/차감 포인트 (양수: 충전, 음수: 차감)

    @Enumerated(EnumType.STRING)
    @Column(name = "point_target")
    private PointTarget pointTarget;  // 포인트 사용 대상 (예약, 이벤트)

    @Column(name = "point_result", nullable = false)
    private Integer pointResult;  // 포인트 잔액 결과

    @Column(name = "point_created_at", nullable = false, updatable = false)
    private LocalDateTime pointCreatedAt;  // 생성일시

    @PrePersist
    public void prePersist() {
        this.pointCreatedAt = LocalDateTime.now();
    }
}
