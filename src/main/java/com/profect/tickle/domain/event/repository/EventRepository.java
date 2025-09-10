package com.profect.tickle.domain.event.repository;

import com.profect.tickle.domain.event.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {


    interface AccrueRow {
        Integer getEventAccrued();
        Long getStatusId();
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
      UPDATE event
         SET event_accrued = event_accrued + :delta,
             status_id = CASE
                           WHEN event_accrued + :delta >= event_goal_price
                             THEN :completed
                           ELSE status_id
                         END
       WHERE event_id = :eventId
         AND status_id = :inProgress
         AND event_accrued < event_goal_price
      RETURNING event_accrued AS eventAccrued, status_id AS statusId
      """, nativeQuery = true)
    List<AccrueRow> accrueAndMaybeComplete(@Param("eventId") Long eventId,
                                           @Param("delta") int delta,
                                           @Param("inProgress") Long inProgress,
                                           @Param("completed") Long completed);

    @Query("select s.id from Seat s where s.event.id = :eventId")
    Long findSeatIdByEventId(@Param("eventId") Long eventId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findForUpdateById(@Param("id") Long id);

    // ★ 쿠폰 경로(신규): @Where 무시용 네이티브 + for update
    @Query(value = "select * from event e where e.event_id = :id for update", nativeQuery = true)
    Optional<Event> findAnyTypeForUpdateById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Event e
           set e.accrued = e.accrued + :delta
         where e.id = :eventId
           and e.status.id = :inProgress
    """)

    int incrementAccrued(@Param("eventId") Long eventId,
                         @Param("delta") int delta,
                         @Param("inProgress") Long inProgress);


/*    @Query("select s.id from Seat s where s.event.id = :eventId")
    Long findSeatIdByEventId(@Param("eventId") Long eventId);*/


    // Postgres면 증가 후 값을 곧바로 받는 버전 권장 (더 깔끔)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update event
           set event_accrued = event_accrued + :delta
         where event_id = :eventId and status_id = :inProgress
        returning event_accrued
    """, nativeQuery = true)
    List<Integer> incrementAccruedReturning(@Param("eventId") Long eventId,
                                            @Param("delta") int delta,
                                            @Param("inProgress") Long inProgress);

}
