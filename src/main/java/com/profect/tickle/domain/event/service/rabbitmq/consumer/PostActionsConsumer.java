package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.service.application.PostActionsService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostPointHistoryMessage;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
import com.profect.tickle.domain.point.entity.PointTarget;
import com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostActionsConsumer {

    private final PostActionsService postActionsService;

    @RabbitListener(queues = RabbitMQEventConfig.POST_POINT_QUEUE, concurrency = "8")
    public void handlePointHistory(PostPointHistoryMessage msg) {
        postActionsService.recordPointHistory(msg.memberId(), msg.perPrice(), PointTarget.EVENT);
    }

    @RabbitListener(queues = RabbitMQEventConfig.POST_RESERVATION_QUEUE, concurrency = "8")
    public void handleReservation(PostReservationMessage msg) {
        postActionsService.reserveSeatAndCreateReservation(msg.seatId(), msg.memberId(), msg.accrued());
    }
}