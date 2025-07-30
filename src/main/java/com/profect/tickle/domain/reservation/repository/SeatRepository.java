package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.reservation.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
}