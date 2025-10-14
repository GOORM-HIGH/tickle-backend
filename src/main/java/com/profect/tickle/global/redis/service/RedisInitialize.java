package com.profect.tickle.global.redis.service;

import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.global.status.StatusIds;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisInitialize {

    private final RedisTemplate<String, Object> redisTemplate;

    public void initializeRedisEvent(Event event) {
        String key = "event:" + event.getId();

        redisTemplate.opsForHash().put(key, "accrued", 0);
        redisTemplate.opsForHash().put(key, "target", event.getAccrued());
        redisTemplate.opsForHash().put(key, "status", "IN_PROGRESS");
        redisTemplate.opsForHash().put(key, "statusId", StatusIds.Event.IN_PROGRESS.toString());
    }
}
