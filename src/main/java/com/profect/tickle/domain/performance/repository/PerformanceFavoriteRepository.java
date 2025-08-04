package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.PerformanceFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerformanceFavoriteRepository extends JpaRepository<PerformanceFavorite, Long> {
    boolean existsByMemberIdAndPerformanceId(Long memberId, Long performanceId);
    Optional<PerformanceFavorite> findByMemberIdAndPerformanceId(Long memberId, Long performanceId);
}
