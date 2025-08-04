package com.profect.tickle.domain.performance.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "performance_favorite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceFavorite {
    @Id
    @Column(name = "performance_favorite_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "performance_favorite_created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PerformanceFavorite from(Member member, Performance performance) {
        PerformanceFavorite favorite = new PerformanceFavorite();
        favorite.member = member;
        favorite.performance = performance;
        favorite.createdAt = LocalDateTime.now();
        return favorite;
    }
}
