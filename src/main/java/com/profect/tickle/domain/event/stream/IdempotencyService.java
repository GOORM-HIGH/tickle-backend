package com.profect.tickle.domain.event.stream;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/** 동일 (eventId, memberId) 요청의 중복 처리를 막기 위한 간단한 멱등 키 */
//@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedissonClient redisson;

    public boolean tryMarkProcessed(Long eventId, Long memberId) {
        String key = "idem:ticket:" + eventId + ":" + memberId;
        RBucket<String> bucket = redisson.getBucket(key);
        // 존재하지 않으면 "1"로 세팅 + TTL 6시간 설정 → true 반환
        return bucket.trySet("1", 6, TimeUnit.HOURS);
    }
}