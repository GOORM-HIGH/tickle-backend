package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.service.application.EventCoreLockService;
import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostPointHistoryMessage;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
import com.profect.tickle.domain.event.service.rabbitmq.producer.PostActionsProducer;
import com.profect.tickle.global.status.StatusIds;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
public class TicketEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostActionsProducer postProducer;
    private final EventCoreLockService eventCoreLockService;
    private final String luaScript;

    public TicketEventConsumer(RedisTemplate<String, Object> redisTemplate,
                               PostActionsProducer postProducer,
                               EventCoreLockService eventCoreLockService) throws IOException {
        this.redisTemplate = redisTemplate;
        this.postProducer = postProducer;
        this.eventCoreLockService = eventCoreLockService;

        try (InputStream is = new ClassPathResource("scripts/event_decrement.lua").getInputStream()) {
            this.luaScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        log.info("Loaded Lua script for event decrement logic.");
    }


    @RabbitListener(
            queues = {
                    "event.shard.queue.0", "event.shard.queue.1",
                    "event.shard.queue.2", "event.shard.queue.3",
                    "event.shard.queue.4", "event.shard.queue.5",
                    "event.shard.queue.6", "event.shard.queue.7"
            },
            concurrency = "8" // 샤드당 순차 처리
    )
    public void handleTicketApply(ApplyRequestDto request) {
        Long eventId = request.getEventId();
        Long memberId = request.getMemberId();
        String key = "event:" + eventId;

        try {
            // 1. Redis Lua Script 실행
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
            Long newAccrued = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    request.getPerPrice(),              // ARGV[1] : perPrice
                    StatusIds.Event.COMPLETED           // ARGV[2] : COMPLETED
            );

            if (newAccrued == -99999) {
                log.error("이미 종료된 이벤트입니다. eventId={}, memberId={}", eventId, memberId);
                return;
            }
            // 3. 후처리 (비동기 메시지 발행)
            postProducer.sendPointHistory(new PostPointHistoryMessage(memberId, request.getPerPrice()));

            if (newAccrued <= 0) {
                // Redis 내부에서는 이미 status=COMPLETED 로 변경됨
                eventCoreLockService.completeEvent(eventId);
                postProducer.sendReservation(new PostReservationMessage(memberId, null, 0));
                log.info("이벤트 종료 처리 완료! eventId={}, 남은 목표금액={}", eventId, newAccrued);
            }
        } catch (Exception e) {
            log.error("Redis Lua 처리 실패 eventId={}, memberId={}", eventId, memberId, e);
        }
    }
}