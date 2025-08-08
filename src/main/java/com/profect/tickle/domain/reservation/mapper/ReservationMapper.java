package com.profect.tickle.domain.reservation.mapper;

import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ReservationMapper {

    List<ReservedSeatDto> findReservedSeatById(Long reservationId);

    Optional<ReservationDto> findById(@Param("reservationId") long reservationId);

    List<ReservationDto> findByPerformanceId(@Param("performanceId") Long performanceId);
}
