package com.profect.tickle.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
public class AppConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Supplier<UUID> uuidSupplier() {
        return UUID::randomUUID;
    }
}
