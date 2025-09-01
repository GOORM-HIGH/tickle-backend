package com.profect.tickle.domain.event.repository;

import com.profect.tickle.domain.event.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findForUpdateById(@Param("id") Long id);

    // ★ 쿠폰 경로(신규): @Where 무시용 네이티브 + for update
    @Query(value = "select * from event e where e.event_id = :id for update", nativeQuery = true)
    Optional<Event> findAnyTypeForUpdateById(@Param("id") Long id);
}
