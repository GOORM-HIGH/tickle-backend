package com.profect.tickle.domain.reservation.dto.response.reservation;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationServiceDto {

    private Long id;

    private Long memberId;

    private String memberEmail;

    private Long performanceId;

    private Long statusId;

    private String code;

    private Integer price;

    private Boolean isNotify;

    private Instant createdAt;

    private Instant updatedAt;

    @Setter
    private List<ReservedSeatDto> seatList;
}
