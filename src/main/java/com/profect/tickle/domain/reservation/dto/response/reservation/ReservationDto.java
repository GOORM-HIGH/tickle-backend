package com.profect.tickle.domain.reservation.dto.response.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDto {

    private Long id;
    private String code;
    private Integer totalPrice;
    private Boolean isNotify;
    @Setter
    private List<ReservedSeatDto> seatList;

}
