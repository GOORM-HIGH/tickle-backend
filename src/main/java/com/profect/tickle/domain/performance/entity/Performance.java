package com.profect.tickle.domain.performance.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "performance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance {

    @Id
    @Column(name = "performance_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "performance_title", length = 50, nullable = false)
    private String title;

    @Column(name = "performance_price", nullable = false)
    private Short price;

    @Column(name = "performance_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "performance_runtime", nullable = false)
    private Short runtime;

    @Column(name = "performance_img", length = 255, nullable = false)
    private String img;

    @Column(name = "performance_start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "performance_end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "performance_is_event", nullable = false)
    private Boolean isEvent;

    @Column(name = "performance_look_count", nullable = false)
    private Short lookCount;

    @Column(name = "performance_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "performance_updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
