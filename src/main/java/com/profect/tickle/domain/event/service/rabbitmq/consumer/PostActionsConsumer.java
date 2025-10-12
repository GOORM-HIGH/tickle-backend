package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.service.application.PostActionsService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostActionsMessage;
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

    @RabbitListener(queues = RabbitMQEventConfig.POST_EVENT_QUEUE)
    public void handlePostActions(PostActionsMessage msg) {
        try {
            postActionsService.recordPointHistory(msg.getMemberId(), msg.getPerPrice(), PointTarget.EVENT);

            if (msg.isWinner()) {
                postActionsService.reserveSeatAndCreateReservation(
                        msg.getSeatId(), msg.getMemberId(), msg.getAccrued());
            }

            log.info("✅ 후속 작업 완료: memberId={}, winner={}", msg.getMemberId(), msg.isWinner());
        } catch (Exception e) {
            log.error("❌ 후속 작업 실패: {}", msg, e);
        }
    }
}