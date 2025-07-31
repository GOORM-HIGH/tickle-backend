package com.profect.tickle.domain.event.mapper;

import com.profect.tickle.domain.event.dto.response.CouponListResponseDto;
import com.profect.tickle.domain.event.dto.response.TicketListResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMapper {
    List<TicketListResponseDto> findTicketEventList(@Param("size") int size, @Param("offset") int offset);
    List<CouponListResponseDto> findCouponEventList(@Param("size") int size, @Param("offset") int offset);

    long countCouponEvents(); // 페이징처리 시, 전체 쿠폰의 갯수를 알기 위한 메서드
    long countTicketEvents(); // 페이징처리 시, 전체 티켓의 갯수를 알기 위한 메서드
}
