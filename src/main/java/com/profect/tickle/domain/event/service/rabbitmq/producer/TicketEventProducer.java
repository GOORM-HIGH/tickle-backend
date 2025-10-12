package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig.EVENT_EXCHANGE;
import static com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig.EVENT_ROUTING_KEY;


@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendApplyRequest(Long eventId, Long memberId) {
        log.error("Producer 호출 완료");

        ApplyRequestDto request = ApplyRequestDto.create(eventId, memberId);
        rabbitTemplate.convertAndSend(EVENT_EXCHANGE, EVENT_ROUTING_KEY, request);
    }
}