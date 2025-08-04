package com.profect.tickle.domain.point.repository;

import com.profect.tickle.domain.point.entity.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<Point, Integer> {
    Page<Point> findByMemberId(Long memberId, Pageable pageable);
}
