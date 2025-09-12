package com.profect.tickle.global.redis.dto;

import java.time.Duration;

public record RedisDto (
    String key,
    String value,
    Duration duration
){}

