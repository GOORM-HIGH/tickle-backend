package com.profect.tickle.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        var ex = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);           // 최소 스레드
        ex.setMaxPoolSize(10);           // 최대 스레드
        ex.setQueueCapacity(200);        // 대기열 크기
        ex.setKeepAliveSeconds(30);      // 유휴 스레드 유지 시간
        ex.setThreadNamePrefix("mail-"); // 로그 식별에 유용
        ex.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 대기
        ex.setAwaitTerminationSeconds(30);
        // 필요시 거부 정책 (기본: AbortPolicy = 예외)
        ex.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    // config
    @Bean(name = "sseExecutor")
    public Executor sseExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("sse-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.initialize();
        return ex;
    }
}
