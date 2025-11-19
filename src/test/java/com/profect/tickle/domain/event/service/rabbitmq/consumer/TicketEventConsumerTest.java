package com.profect.tickle.domain.event.service.rabbitmq.consumer;

import com.profect.config.RedisTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TicketEventConsumerTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> script;

    @BeforeEach
    void setUp() throws IOException {
        // Lua 스크립트 로드
        try (InputStream is = new ClassPathResource("scripts/event_decrement.lua").getInputStream()) {
            String lua = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            this.script = new DefaultRedisScript<>(lua, Long.class);
        }

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // 초기 상태 세팅
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("target", 100);           // 목표금액 100
        eventData.put("status", "IN_PROGRESS"); // 진행중 상태
        eventData.put("statusId", 1);           // (임의)
        redisTemplate.opsForHash().putAll("event:1", eventData);
    }

    @Test
    void testLuaAtomicDecrement() {
        String key = "event:1";
        Long perPrice = 1L;
        String completed = "COMPLETED";

        long lastResult = 0L;
        int completedAt = 0;

        // 1~110명의 유저 요청 시뮬레이션
        for (int i = 1; i <= 110; i++) {
            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    perPrice,
                    completed
            );

            if (result == -99999) {
                System.out.printf("🚫 %d번째 요청 실패 (이벤트 종료됨)%n", i);
                continue;
            }

            if (result <= 0 && completedAt == 0) {
                completedAt = i;
                System.out.printf("🎯 %d번째 요청에서 목표 달성! result=%d%n", i, result);
            }

            lastResult = result;
        }

        // ✅ 검증
        assertEquals(0L, lastResult, "마지막 감소 결과는 0이어야 한다");
        assertEquals(100, completedAt, "정확히 100번째 요청에서 이벤트가 종료되어야 한다");

        // ✅ 이후 상태 검증
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        assertEquals("COMPLETED", status, "이벤트 상태가 COMPLETED로 변경되어야 한다");
    }
}