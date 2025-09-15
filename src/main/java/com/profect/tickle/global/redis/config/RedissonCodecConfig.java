package com.profect.tickle.global.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonCodecConfig {

    @Bean
    public TypedJsonJacksonCodec streamFieldMapCodec() {
        return new TypedJsonJacksonCodec(String.class, Object.class);
    }
}
