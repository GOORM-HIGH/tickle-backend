package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.tickle.domain.event.service.rabbitmq.dto.ApplyRequestDto;
import com.profect.tickle.domain.event.service.rabbitmq.dto.PostReservationMessage;
import com.profect.tickle.domain.event.service.rabbitmq.producer.PostActionsProducer;
import com.profect.tickle.global.status.StatusIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostActionsProducer postProducer;

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
            Long newAccrued = redisTemplate.opsForHash().increment(key, "accrued", 1);

            Object targetObj = redisTemplate.opsForHash().get(key, "target");
            if (targetObj == null) {
                log.error("⚠️ target 값이 Redis에 없음 (eventId={})", eventId);
                return;
            }

            long target = Long.parseLong(targetObj.toString());
            if (newAccrued >= target) {
                redisTemplate.opsForHash().put(key, "statusId", StatusIds.Event.COMPLETED);

                // 좌석 예약 메시지 발송
                postProducer.sendReservation(new PostReservationMessage(memberId, null, newAccrued.intValue()));
                log.info("🎯 이벤트 종료! eventId={}, 누적={}/{}", eventId, newAccrued, target);
            }

        } catch (Exception e) {
            log.error("❌ Redis 누적 처리 실패 eventId={}, memberId={}", eventId, memberId, e);
        }
    }
}