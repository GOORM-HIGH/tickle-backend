package com.profect.tickle.domain.event.service.rabbitmq.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Core 처리 후 후속(Post) 단계에서 사용하는 메시지 DTO
 * - 포인트 차감 및 예매권 발급(우승자일 경우)을 위한 데이터 전송용
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PostActionsMessage {

    /**
     * 응모한 사용자 ID
     */
    private Long memberId;

    /**
     * 이벤트 1회 참여 시 차감 포인트
     */
    private int perPrice;

    /**
     * 당첨 여부
     */
    private boolean winner;

    /**
     * 좌석 ID (우승자만 존재)
     */
    private Long seatId;

    /**
     * 누적 포인트 (좌석 예약 시 사용)
     */
    private int accrued;
}