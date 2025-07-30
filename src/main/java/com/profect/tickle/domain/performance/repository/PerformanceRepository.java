package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance,Long> {
}
