package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.reservation.entity.SeatGrade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

public record TicketEventDetailResponseDto(

        @Schema(description = "이벤트 ID", example = "1")
        Long id,

        @Schema(description = "공연 이름", example = "뮤지컬 <레미제라블>")
        String performanceTitle,

        @Schema(description = "공연 장소", example = "예술의전당 오페라극장")
        String performancePlace,

        @Schema(description = "공연 시간", example = "110")
        Short performanceRuntime,

        @Schema(description = "공연 기간", example = "2025-08-01")
        Instant performanceDate,

        @Schema(description = "이벤트 좌석 코드", example = "A12")
        String seatNumber,

        @Schema(description = "좌석 등급", example = "VIP")
        SeatGrade seatGrade,

        @Schema(description = "응모 가격", example = "5000")
        Short perPrice,

        @Schema(description = "공연 이미지 URL", example = "https://example.com/perf.jpg")
        String performanceImg,

        @Schema(description = "이벤트 상태명", example = "진행중")
        String eventStatusName
) {}