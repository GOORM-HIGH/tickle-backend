package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance,Long> {
    @Query("SELECT p FROM Performance p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Performance> findActiveById(@Param("id") Long id);
    @Query("SELECT p FROM Performance p WHERE p.deletedAt IS NULL")
    List<Performance> findAllActive();
    boolean existsByTitleAndDate(String title, Instant date);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE performance p
       SET status_id = CASE
                         WHEN sub.kst_date > sub.today_kst THEN 1  -- 공연예정
                         WHEN sub.kst_date = sub.today_kst THEN 2  -- 공연진행
                         ELSE 3                                    -- 공연종료
                       END
      FROM (
         SELECT performance_id,
                (performance_date AT TIME ZONE 'Asia/Seoul')::date AS kst_date,
                (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date AS today_kst
           FROM performance
      ) sub
     WHERE p.performance_id = sub.performance_id
       AND p.performance_deleted_at IS NULL       -- 삭제 안 된 공연만
       AND p.status_id <> 3                       -- 이미 종료 상태는 제외
       AND p.status_id IS DISTINCT FROM CASE      -- 상태가 달라질 때만 갱신
                                          WHEN sub.kst_date > sub.today_kst THEN 1
                                          WHEN sub.kst_date = sub.today_kst THEN 2
                                          ELSE 3
                                        END
    """, nativeQuery = true)
    int updateAllStatusesByDateRule();
}
