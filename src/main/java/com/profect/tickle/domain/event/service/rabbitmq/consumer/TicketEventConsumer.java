package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.application.EventCoreLockService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostPointHistoryMessage;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
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

    @RabbitListener(queues = RabbitMQEventConfig.EVENT_QUEUE, concurrency = "4")
    public void handleTicketApply(ApplyRequestDto request) {
        Long eventId = request.getEventId();
        Long memberId = request.getMemberId();

        try {
            EventDecision d = eventCoreLockService.applyCore(eventId, memberId);

            // 1️⃣ 포인트 이력 후속 큐로 전송
            postProducer.sendPointHistory(new PostPointHistoryMessage(memberId, d.perPrice()));

            // 2️⃣ 당첨자만 좌석 예약 큐로 전송
            if (d.winner()) {
                postProducer.sendReservation(
                        new PostReservationMessage(memberId, d.seatId(), d.accrued())
                );
            }

            log.info("✅ Core 처리 완료 후 후속 메시지 전송 완료: memberId={}, winner={}", memberId, d.winner());
        } catch (Exception e) {
            log.error("❌ 응모 실패: eventId={}, memberId={}", eventId, memberId, e);
        }
    }
}