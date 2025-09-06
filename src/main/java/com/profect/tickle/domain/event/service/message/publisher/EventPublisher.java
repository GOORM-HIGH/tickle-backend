package com.profect.tickle.domain.event.service.message.publisher;

import com.profect.tickle.domain.event.service.message.dto.TicketLockMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final RedissonClient redissonClient;
    private static final String TOPIC_NAME = "ticket-event-topic";

    public void publish(TicketLockMessage msg) {
        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new JsonJacksonCodec());
        topic.publishAsync(msg)
                .thenAccept(r -> log.info("Published ticket event: {}", msg));
    }
}