package com.profect.tickle.domain.reservation.mapper;

import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReservationMapper {

    List<ReservedSeatDto> findReservedSeatById(Long reservationId);
}
