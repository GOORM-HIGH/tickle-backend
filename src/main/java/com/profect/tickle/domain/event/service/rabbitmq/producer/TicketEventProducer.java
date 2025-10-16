package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
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
    private final EventRepository eventRepository;

    public void sendApplyRequest(Long memberId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        ApplyRequestDto request = ApplyRequestDto.create(eventId, memberId, event.getGoalPrice(), event.getAccrued(), event.getPerPrice());

        int shardIndex = (int) (eventId % EVENT_SHARD_COUNT);
        String routingKey = EVENT_ROUTING_KEY_PREFIX + shardIndex;

        rabbitTemplate.convertAndSend(EVENT_EXCHANGE, routingKey, request);
    }
}