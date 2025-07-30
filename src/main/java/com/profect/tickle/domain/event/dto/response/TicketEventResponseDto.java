package com.profect.tickle.domain.event.dto.response;

import com.profect.tickle.domain.event.entity.Event;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "티켓 이벤트 응답 DTO")
public record TicketEventResponseDto(

        @Schema(description = "이벤트 ID", example = "1001")
        Long eventId,

        @Schema(description = "공연 ID", example = "12")
        Long performanceId,

        @Schema(description = "이벤트명", example = "기프트 티켓 이벤트")
        String eventName,

        @Schema(description = "좌석 정보", example = "A열 3번")
        String ticketSeat
) {
    public static TicketEventResponseDto from(Event event, Long performanceId) {
        String ticketSeat = event.getSeat().getSeatClass().getGrade() + "열 "
                + event.getSeat().getSeatNumber() + "번";     // "A열 45번"

        return new TicketEventResponseDto(
                event.getId(),
                performanceId,
                event.getName(),
                ticketSeat
        );
    }
}