package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.reservation.entity.SeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {
    @Query("SELECT MIN(s.price) FROM SeatTemplate s WHERE s.hallType = :hallType")
    Integer findMinPriceByHallType(@Param("hallType") String hallType);

    @Query("SELECT MAX(s.price) FROM SeatTemplate s WHERE s.hallType = :hallType")
    Integer findMaxPriceByHallType(@Param("hallType") String hallType);
}
