package com.profect.tickle.domain.event.mapper;

import com.profect.tickle.domain.event.dto.response.CouponListResponseDto;
import com.profect.tickle.domain.event.dto.response.ExpiringSoonCouponResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface CouponMapper {
    List<ExpiringSoonCouponResponseDto> findCouponListExpiringBefore(@Param("targetDate") Instant targetDate);
    CouponListResponseDto findCouponById(@Param("couponId") Long couponId);
}
