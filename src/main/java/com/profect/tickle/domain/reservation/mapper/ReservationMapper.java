package com.profect.tickle.domain.reservation.mapper;

import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ReservationMapper {

    List<ReservedSeatDto> findReservedSeatListByReservationId(
            @Param("reservationId") Long reservationId);

    Optional<ReservationServiceDto> findById(
            @Param("reservationId") Long reservationId);

    List<ReservationServiceDto> findByPerformanceId(
            @Param("performanceId") Long performanceId);
}
