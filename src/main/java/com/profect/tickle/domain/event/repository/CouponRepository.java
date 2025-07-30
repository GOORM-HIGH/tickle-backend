package com.profect.tickle.domain.event.repository;

import com.profect.tickle.domain.event.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByName(String name);
}
