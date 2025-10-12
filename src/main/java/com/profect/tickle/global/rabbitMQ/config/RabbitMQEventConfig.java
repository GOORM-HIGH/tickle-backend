package com.profect.tickle.global.rabbitMQ.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventQueue() {
        return QueueBuilder.durable(EVENT_QUEUE)
                .withArgument("x-message-ttl", 300000)
                .withArgument("x-max-length", 10000)
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