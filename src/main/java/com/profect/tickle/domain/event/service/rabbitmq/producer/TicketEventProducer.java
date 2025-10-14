package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendApplyRequest(Long memberId, Long eventId) {

        ApplyRequestDto request = ApplyRequestDto.create(eventId, memberId);

        int shardIndex = (int) (eventId % EVENT_SHARD_COUNT);
        String routingKey = EVENT_ROUTING_KEY_PREFIX + shardIndex;

        rabbitTemplate.convertAndSend(EVENT_EXCHANGE, routingKey, request);
    }
}