/*
package com.profect.tickle.domain.event.service.rabbitmq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.event.dto.EventDecision;
import com.profect.tickle.domain.event.service.application.EventCoreLockService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostPointHistoryMessage;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
import com.profect.tickle.global.rabbitMQ.config.RabbitMQEventConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketApplyBatchProcessor {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final EventCoreLockService eventCoreLockService;
    private final PostActionsProducer postProducer;

    @Scheduled(fixedDelay = 100)
    public void batchConsume() {
        int maxBatchSize = 1000;
        List<ApplyRequestDto> requests = new ArrayList<>();

        for (int i = 0; i < maxBatchSize; i++) {
            Object raw = rabbitTemplate.receiveAndConvert(RabbitMQEventConfig.EVENT_QUEUE);
            if (raw == null) break;

            try {
                ApplyRequestDto dto = objectMapper.convertValue(raw, ApplyRequestDto.class);
                requests.add(dto);
            } catch (Exception e) {
                log.error("메시지 역직렬화 실패", e);
            }
        }

        if (requests.isEmpty()) return;

        log.info("배치 응모 처리 시작 - size: {}", requests.size());

        for (ApplyRequestDto request : requests) {
            try {
                EventDecision d = eventCoreLockService.applyCore(request.getEventId(), request.getMemberId());

                postProducer.sendPointHistory(new PostPointHistoryMessage(request.getMemberId(), d.perPrice()));

                if (d.winner()) {
                    postProducer.sendReservation(new PostReservationMessage(request.getMemberId(), d.seatId(), d.accrued()));
                }

                log.error("memberId={} 처리 완료 (당첨: {})", request.getMemberId(), d.winner());
            } catch (Exception e) {
                log.error("배치 처리 중 오류: memberId={}, eventId={}", request.getMemberId(), request.getEventId(), e);
            }
        }
    }
}
*/
