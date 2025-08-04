package com.profect.tickle.domain.event.mapper;

import com.profect.tickle.domain.event.dto.response.ExpiringSoonCouponResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CouponMapper {
    List<ExpiringSoonCouponResponseDto> findCouponsExpiringBefore(@Param("targetDate") LocalDate targetDate);
}
