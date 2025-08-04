package com.profect.tickle.global.status.repository;

import com.profect.tickle.global.status.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByTypeAndCode(String type, Short code);
}
