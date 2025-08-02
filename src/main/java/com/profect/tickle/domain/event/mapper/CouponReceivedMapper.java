package com.profect.tickle.domain.event.mapper;

import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponReceivedMapper {
    List<CouponResponseDto> findMyCoupons(@Param("memberId") Long memberId,
                                          @Param("size") int size,
                                          @Param("offset") int offset);
    int countMyCoupons(@Param("memberId") Long memberId);
}