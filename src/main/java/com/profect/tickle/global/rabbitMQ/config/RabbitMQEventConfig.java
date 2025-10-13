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

    // ───────────────────────────────
    // [1] 티켓 응모 이벤트
    // ───────────────────────────────
    public static final String EVENT_EXCHANGE = "event.exchange";
    public static final String EVENT_QUEUE = "event.ticket.queue";
    public static final String EVENT_ROUTING_KEY = "event.ticket";

    // ───────────────────────────────
    // [2] 후속 작업 (PostActions)
    // ───────────────────────────────
    public static final String POST_POINT_EXCHANGE = "post.point.exchange";
    public static final String POST_POINT_QUEUE = "post.point.queue";
    public static final String POST_POINT_ROUTING_KEY = "post.point";

    public static final String POST_RESERVATION_EXCHANGE = "post.reservation.exchange";
    public static final String POST_RESERVATION_QUEUE = "post.reservation.queue";
    public static final String POST_RESERVATION_ROUTING_KEY = "post.reservation";


    // ───────────────────────────────
    // [Exchange / Queue / Binding]
    // ───────────────────────────────
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
        return BindingBuilder.bind(eventQueue()).to(eventExchange()).with(EVENT_ROUTING_KEY);
    }

    // ───────────────────────────────
    // Post: PointHistory
    // ───────────────────────────────
    @Bean
    public TopicExchange postPointExchange() {
        return new TopicExchange(POST_POINT_EXCHANGE, true, false);
    }

    @Bean
    public Queue postPointQueue() {
        return QueueBuilder.durable(POST_POINT_QUEUE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Binding postPointBinding() {
        return BindingBuilder.bind(postPointQueue())
                .to(postPointExchange())
                .with(POST_POINT_ROUTING_KEY);
    }

    // ───────────────────────────────
    // Post: Reservation
    // ───────────────────────────────
    @Bean
    public TopicExchange postReservationExchange() {
        return new TopicExchange(POST_RESERVATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue postReservationQueue() {
        return QueueBuilder.durable(POST_RESERVATION_QUEUE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Binding postReservationBinding() {
        return BindingBuilder.bind(postReservationQueue())
                .to(postReservationExchange())
                .with(POST_RESERVATION_ROUTING_KEY);
    }
}