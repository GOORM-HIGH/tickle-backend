package com.profect.tickle.global.config;

import com.profect.tickle.global.websocket.StompJwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP + SockJS WebSocket 설정 (성능 최적화)
 * 기존 WebSocketConfig와 별도로 작동
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 최적화 (대규모 연결 대응)
        config.enableSimpleBroker("/topic", "/queue")
              .setTaskScheduler(heartBeatScheduler())
              .setHeartbeatValue(new long[]{60000, 60000}); // 60초 하트비트 (부하 대폭 감소)
        
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(256 * 1024) // 256KB 스트림 제한 (메모리 절약)
                .setHttpMessageCacheSize(500)    // 메시지 캐시 크기 감소
                .setDisconnectDelay(60 * 1000);  // 60초 연결 유지 (안정성 향상)
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // WebSocket 전송 최적화
        registration.setMessageSizeLimit(64 * 1024)      // 64KB 메시지 크기 제한
                   .setSendBufferSizeLimit(512 * 1024)   // 512KB 송신 버퍼
                   .setSendTimeLimit(10 * 1000);         // 10초 송신 타임아웃
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 스레드 풀 최적화 (대규모 연결 대응)
        registration.taskExecutor().corePoolSize(100)
                   .maxPoolSize(500)
                   .queueCapacity(2000);
        
        registration.interceptors(stompJwtChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 송신 채널 스레드 풀 최적화 (대규모 연결 대응)
        registration.taskExecutor().corePoolSize(100)
                   .maxPoolSize(500)
                   .queueCapacity(2000);
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20); // 하트비트 스레드 증가
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }

}

