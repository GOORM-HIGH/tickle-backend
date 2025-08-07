package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.reservation.entity.Seat;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Seat> findAllByIdWithLock(@Param("seatIds") List<Long> seatIds);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.preemptUserId = :userId AND s.preemptedUntil > :now")
    int countByPreemptUserIdAndPreemptedUntilAfter(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("SELECT s.id FROM Seat s WHERE s.preemptUserId = :userId AND s.id IN :seatIds AND s.preemptedUntil > :now")
    List<Long> findPreemptedSeatIdsByUserAndSeatIds(@Param("userId") Long userId,
            @Param("seatIds") List<Long> seatIds);

    @Query("SELECT s FROM Seat s WHERE s.preemptionToken = :token")
    List<Seat> findByPreemptionToken(@Param("token") String preemptionToken);

    @Query("SELECT s FROM Seat s WHERE s.preemptionToken = :token")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Seat> findByPreemptionTokenWithLock(@Param("token") String preemptionToken);

    List<Seat> findByReservationId(Long reservationId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Seat s
        SET s.preemptUserId = null,
            s.preemptionToken = null,
            s.preemptedAt = null,
            s.preemptedUntil = null,
            s.status.id = 11L
        WHERE s.preemptedUntil IS NOT NULL
          AND s.preemptedUntil < :now
    """)
    int clearExpiredPreemptionsBulk(@Param("now") Instant now);
}