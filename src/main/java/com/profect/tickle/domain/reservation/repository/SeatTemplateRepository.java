package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.reservation.entity.SeatTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    List<SeatTemplate> findByHallType(HallType hallType);

    @Query("SELECT MIN(s.price) FROM SeatTemplate s WHERE s.hallType = :hallType")
    Integer findMinPriceByHallType(@Param("hallType") String hallType);

    @Query("SELECT MAX(s.price) FROM SeatTemplate s WHERE s.hallType = :hallType")
    Integer findMaxPriceByHallType(@Param("hallType") String hallType);

}
