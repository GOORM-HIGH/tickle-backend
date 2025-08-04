package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.CouponReceived;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponReceivedRepository extends JpaRepository<CouponReceived, Long> {
    boolean existsByMemberIdAndCouponId(Long id, Long id1);

    @Query("SELECT cr FROM CouponReceived cr WHERE cr.coupon.id = :couponId " +
            "AND cr.member.id = :memberId AND cr.status.id = 17")
    Optional<CouponReceived> findByCouponIdAndMemberIdAndNotUsed(@Param("couponId") Long couponId,
            @Param("memberId") Long memberId);
}
