package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.performance.entity.HallType;
import com.profect.tickle.domain.reservation.entity.SeatTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    List<SeatTemplate> findByHallType(HallType hallType);
}
