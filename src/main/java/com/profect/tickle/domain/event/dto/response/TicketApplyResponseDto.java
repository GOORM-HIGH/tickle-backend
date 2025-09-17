package com.profect.tickle.domain.event.dto.response;

public record TicketApplyResponseDto(
        Long eventId,
        Long memberId,
        String message
) {
    public static TicketApplyResponseDto from(Long eventId, Long memberId) {
        return new TicketApplyResponseDto(
                eventId,
                memberId,
                "성공적으로 응모되었습니다. 마이페이지에서 결과를 확인하세요."
        );
    }
}