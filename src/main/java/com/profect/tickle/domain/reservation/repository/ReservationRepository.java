package com.profect.tickle.domain.reservation.repository;

import com.profect.tickle.domain.reservation.entity.Reservation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 사용자별 예매 내역 조회
    Page<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Reservation> findByIdAndMemberId(Long reservationId, Long memberId);
}
