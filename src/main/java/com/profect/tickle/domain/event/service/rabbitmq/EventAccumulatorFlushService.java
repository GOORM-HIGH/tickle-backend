package com.profect.tickle.domain.event.service.rabbitmq;

import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventAccumulatorFlushService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;
    private final StatusProvider statusProvider;

    @Transactional
    public void flushToDB() {
        Set<String> keys = redisTemplate.keys("event:*");
        if (keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long eventId = Long.parseLong(key.replace("event:", ""));
                Object accruedObj = redisTemplate.opsForHash().get(key, "target");
                Object statusIdObj = redisTemplate.opsForHash().get(key, "statusId"); // status → statusId 로 변경

                if (accruedObj == null) continue;

                int accrued = Integer.parseInt(accruedObj.toString());
                Long statusId = (statusIdObj != null)
                        ? Long.parseLong(statusIdObj.toString())
                        : StatusIds.Event.IN_PROGRESS;

                Status status = statusProvider.provide(statusId);

                eventRepository.updateAccruedAndStatus(eventId, accrued, status);
                log.error("💾 [Flush] DB 반영 완료 eventId={}, accrued={}, status={}", eventId, accrued, status.getCode());

                // 완료된 이벤트는 Redis에서 삭제
                if (StatusIds.Event.COMPLETED.equals(statusId)) {
                    redisTemplate.delete(key);
                    log.info("🧹 Redis에서 이벤트 삭제 eventId={}", eventId);
                }

            } catch (Exception e) {
                log.error("❌ Flush 중 오류 key={}", key, e);
            }
        }
    }
}