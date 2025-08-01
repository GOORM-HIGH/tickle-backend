package com.profect.tickle.domain.event.dto.response;

public record TicketApplyResponseDto(
        Long eventId,
        Long memberId,
        boolean isWinner,
        String message
) {
    public static TicketApplyResponseDto from(Long eventId, Long memberId, boolean isWinner) {
        return new TicketApplyResponseDto(
                eventId,
                memberId,
                isWinner,
                isWinner ? "축하합니다! 티켓에 당첨되었습니다. \n 예매권은 마이페이지에서 확인하세요." : "아쉽네요. 다음 기회에..."
        );
    }
}