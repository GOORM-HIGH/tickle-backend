package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance,Long> {
    @Query("SELECT p FROM Performance p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Performance> findActiveById(@Param("id") Long id);
    @Query("SELECT p FROM Performance p WHERE p.deletedAt IS NULL")
    List<Performance> findAllActive();
    boolean existsByTitleAndDate(String title, LocalDateTime date);
}
