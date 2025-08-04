package com.profect.tickle.domain.reservation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatPreemptionRequest {

    @NotNull
    private Long performanceId;

    @NotEmpty
    @Size(min = 1, max = 5, message = "좌석은 1개 이상 5개 이하로 선택해주세요")
    private List<Long> seatIds;
}