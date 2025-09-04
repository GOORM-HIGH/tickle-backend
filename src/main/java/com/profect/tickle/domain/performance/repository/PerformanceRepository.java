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

    // 인기 공연 TOP10 조회 (조회수 기준)
    @Query("SELECT p FROM Performance p WHERE p.deletedAt IS NULL ORDER BY p.lookCount DESC LIMIT 10")
    List<Performance> findTop10ByClickCount();

    // 장르별 인기 공연 TOP10 조회
    @Query("SELECT p FROM Performance p WHERE p.genre.id = :genreId AND p.deletedAt IS NULL ORDER BY p.lookCount DESC LIMIT 10")
    List<Performance> findTop10ByGenre(@Param("genreId") Long genreId);

    // 오픈 예정 공연 TOP4 조회
    @Query("SELECT p FROM Performance p WHERE p.startDate > :now AND p.deletedAt IS NULL ORDER BY p.startDate ASC LIMIT 4")
    List<Performance> findTop4UpcomingPerformances(@Param("now") LocalDateTime now);

    // 장르별 공연 목록 조회 (페이징)
    @Query("SELECT p FROM Performance p WHERE p.genre.id = :genreId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Performance> findPerformancesByGenre(@Param("genreId") Long genreId, int offset, int limit);

    // 공연 검색 (페이징)
    @Query("SELECT p FROM Performance p WHERE p.title LIKE %:keyword% AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Performance> searchPerformancesByKeyword(@Param("keyword") String keyword, int offset, int limit);

    // 관련 공연 조회 (같은 장르, 제외할 공연 ID)
    @Query("SELECT p FROM Performance p WHERE p.genre.id = :genreId AND p.id != :excludeId AND p.deletedAt IS NULL ORDER BY p.lookCount DESC LIMIT 4")
    List<Performance> findRelatedPerformances(@Param("genreId") Long genreId, @Param("excludeId") Long excludeId);

}
