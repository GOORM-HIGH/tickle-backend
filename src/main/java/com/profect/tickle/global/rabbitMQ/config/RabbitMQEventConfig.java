package com.profect.tickle.global.rabbitMQ.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 티켓 이벤트용 RabbitMQ 설정
 * - Queue, Exchange, Binding 명시적으로 설정
 */
@Configuration
public class RabbitMQEventConfig {

    public static final String EVENT_EXCHANGE = "event.exchange";
    public static final String EVENT_QUEUE = "event.ticket.queue";
    public static final String EVENT_ROUTING_KEY = "event.ticket";

    public static final String POST_EVENT_EXCHANGE = "post.actions.exchange";
    public static final String POST_EVENT_QUEUE = "post.actions.queue";
    public static final String POST_EVENT_ROUTING_KEY = "post.actions";


    @Bean
    public TopicExchange postActionsExchange() {
        return new TopicExchange("post.actions.exchange", true, false);
    }

    @Bean
    public Queue postActionsQueue() {
        return QueueBuilder.durable("post.actions.queue")
                .withArgument("x-message-ttl", 300000) // 메시지 TTL (5분)
                .build();
    }

    @Bean
    public Binding postActionsBinding() {
        return BindingBuilder
                .bind(postActionsQueue())
                .to(postActionsExchange())
                .with("post.actions");
    }

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventQueue() {
        return QueueBuilder.durable(EVENT_QUEUE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Binding eventBinding() {
        return BindingBuilder
                .bind(eventQueue())
                .to(eventExchange())
                .with(EVENT_ROUTING_KEY);
    }
}