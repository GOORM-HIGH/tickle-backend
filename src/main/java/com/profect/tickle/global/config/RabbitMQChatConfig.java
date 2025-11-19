package com.profect.tickle.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정 (메시지 동시 전송 처리용)
 * 
 * 주요 기능:
 * 1. 메시지 동시 전송 처리
 * 2. 메시지 순서 보장
 * 3. 시스템 과부하 시 메시지 대기열 관리
 * 4. 서버 장애 시 메시지 복구
 */
@Configuration
public class RabbitMQChatConfig {

    // ===== 큐 정의 =====
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String CHAT_NOTIFICATION_QUEUE = "chat.notification.queue";
    public static final String CHAT_FILE_QUEUE = "chat.file.queue";

    // ===== 익스체인지 정의 =====
    public static final String CHAT_EXCHANGE = "chat.exchange";

    // ===== 라우팅 키 정의 =====
    public static final String MESSAGE_ROUTING_KEY = "message";
    public static final String NOTIFICATION_ROUTING_KEY = "notification";
    public static final String FILE_ROUTING_KEY = "file";

    /**
     * 채팅 익스체인지 생성 (Topic Exchange)
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    /**
     * 채팅 메시지 큐 생성
     */
    @Bean
    public Queue chatMessageQueue() {
        return QueueBuilder.durable(CHAT_MESSAGE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5분 TTL
                .withArgument("x-max-length", 10000) // 최대 10,000개 메시지
                .build();
    }

    /**
     * 채팅 알림 큐 생성
     */
    @Bean
    public Queue chatNotificationQueue() {
        return QueueBuilder.durable(CHAT_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 600000) // 10분 TTL
                .withArgument("x-max-length", 5000) // 최대 5,000개 메시지
                .build();
    }

    /**
     * 채팅 파일 큐 생성
     */
    @Bean
    public Queue chatFileQueue() {
        return QueueBuilder.durable(CHAT_FILE_QUEUE)
                .withArgument("x-message-ttl", 1800000) // 30분 TTL
                .withArgument("x-max-length", 1000) // 최대 1,000개 메시지
                .build();
    }

    /**
     * 채팅 메시지 큐 바인딩
     */
    @Bean
    public Binding chatMessageBinding() {
        return BindingBuilder
                .bind(chatMessageQueue())
                .to(chatExchange())
                .with(MESSAGE_ROUTING_KEY);
    }

    /**
     * 채팅 알림 큐 바인딩
     */
    @Bean
    public Binding chatNotificationBinding() {
        return BindingBuilder
                .bind(chatNotificationQueue())
                .to(chatExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * 채팅 파일 큐 바인딩
     */
    @Bean
    public Binding chatFileBinding() {
        return BindingBuilder
                .bind(chatFileQueue())
                .to(chatExchange())
                .with(FILE_ROUTING_KEY);
    }
}