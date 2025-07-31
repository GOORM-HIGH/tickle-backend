package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.CouponReceived;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponReceivedRepository extends JpaRepository<CouponReceived, Long> {
    boolean existsByMemberIdAndCouponId(Long id, Long id1);
}
