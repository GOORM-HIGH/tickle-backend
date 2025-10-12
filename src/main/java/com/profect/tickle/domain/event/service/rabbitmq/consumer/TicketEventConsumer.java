package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.application.EventCoreLockService;
import com.profect.tickle.domain.event.service.application.PostActionsService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final EventCoreLockService eventCoreLockService;
    private final PostActionsService postActionsService;

    @RabbitListener(queues = RabbitMQEventConfig.EVENT_QUEUE)
    public void handleTicketApply(ApplyRequestDto request) {
        Long eventId = request.getEventId();
        Long memberId = request.getMemberId();

        log.error("[응모 요청 수신] eventId={}, memberId={}", eventId, memberId);

        try {
            EventDecision eventDecision = eventCoreLockService.applyCore(eventId, memberId);

            postActionsService.recordPointHistory(memberId, eventDecision.perPrice(), PointTarget.EVENT);

            if (eventDecision.winner()) {
                postActionsService.reserveSeatAndCreateReservation(
                        eventDecision.seatId(),
                        memberId,
                        eventDecision.accrued()
                );
            }

            log.error("✅ 응모 처리 완료: memberId={}, winner={}", memberId, eventDecision.winner());

        } catch (Exception e) {
            log.error("❌ 응모 실패", e);
        }
    }
}