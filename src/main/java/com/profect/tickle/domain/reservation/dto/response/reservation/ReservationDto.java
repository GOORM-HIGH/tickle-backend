package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDto {

    private Long id;
    private String code;
    private Integer totalPrice;
    private boolean isNotify;
    @Setter
    private List<ReservedSeatDto> seatList;

}
