package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.application.EventCoreLockService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostActionsMessage;
import com.profect.tickle.domain.event.service.rabbitmq.producer.PostActionsProducer;
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
    private final PostActionsProducer postProducer;

    @RabbitListener(queues = RabbitMQEventConfig.EVENT_QUEUE)
    public void handleTicketApply(ApplyRequestDto request) {
        Long eventId = request.getEventId();
        Long memberId = request.getMemberId();

        try {
            EventDecision d = eventCoreLockService.applyCore(eventId, memberId);

            postProducer.sendPostActions(
                    new PostActionsMessage(
                            request.getMemberId(),
                            d.perPrice(),
                            d.winner(),
                            d.seatId(),
                            d.accrued()
                    )
            );

        } catch (Exception e) {
            log.error("응모 실패", e);
        }
    }
}