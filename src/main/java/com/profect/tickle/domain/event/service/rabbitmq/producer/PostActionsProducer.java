package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.profect.tickle.domain.event.service.rabbitmq.dto.PostPointHistoryMessage;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
import com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostActionsProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 1️⃣ 포인트 이력 후속 작업 전송
     */
    public void sendPointHistory(PostPointHistoryMessage message) {
        log.info("📨 [PostActionsProducer] 포인트 이력 후속 작업 전송: {}", message);
        rabbitTemplate.convertAndSend(
                RabbitMQEventConfig.POST_POINT_EXCHANGE,
                RabbitMQEventConfig.POST_POINT_ROUTING_KEY,
                message
        );
    }

    /**
     * 2️⃣ 좌석 예약 후속 작업 전송
     */
    public void sendReservation(PostReservationMessage message) {
        log.info("📨 [PostActionsProducer] 좌석 예약 후속 작업 전송: {}", message);
        rabbitTemplate.convertAndSend(
                RabbitMQEventConfig.POST_RESERVATION_EXCHANGE,
                RabbitMQEventConfig.POST_RESERVATION_ROUTING_KEY,
                message
        );
    }
}