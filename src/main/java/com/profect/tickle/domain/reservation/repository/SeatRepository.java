package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.reservation.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s " +
            "LEFT JOIN FETCH s.status " +
            "LEFT JOIN FETCH s.reservation " +
            "WHERE s.performance.id = :performanceId " +
            "ORDER BY s.seatNumber")
    List<Seat> findByPerformanceIdOrderBySeatNumber(@Param("performanceId") Long performanceId);
}