package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.profect.tickle.domain.event.service.rabbitmq.dto.PostActionsMessage;
import com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostActionsProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendPostActions(PostActionsMessage message) {
        log.info("📨 [PostActionsProducer] 후속 작업 전송: {}", message);
        rabbitTemplate.convertAndSend(
                RabbitMQEventConfig.POST_EVENT_EXCHANGE,
                RabbitMQEventConfig.POST_EVENT_ROUTING_KEY,
                message
        );
    }
}