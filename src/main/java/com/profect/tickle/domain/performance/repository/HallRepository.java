package com.profect.tickle.domain.performance.repository;

import com.profect.tickle.domain.performance.entity.Hall;
import com.profect.tickle.domain.performance.entity.HallType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HallRepository extends JpaRepository<Hall,Long> {
    Optional<Hall> findByTypeAndAddress(HallType type, String address);
    Optional<Hall> findByAddress(String address);
}
