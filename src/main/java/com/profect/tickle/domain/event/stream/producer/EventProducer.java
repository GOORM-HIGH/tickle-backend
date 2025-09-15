package com.profect.tickle.domain.event.stream.producer;

import com.profect.tickle.domain.event.stream.dto.EventMessage;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventProducer {

    private static final String STREAM_KEY = "stream:ticket-events";
    private final RedissonClient redisson;
    private final TypedJsonJacksonCodec ticketCodec;

    public void appendToStream(EventMessage msg) {
        RStream<String, String> stream =
                redisson.getStream("stream:ticket-events", StringCodec.INSTANCE);

        var args = StreamAddArgs.entries(
                Map.of(
                        "eventId",  String.valueOf(msg.eventId()),
                        "memberId", String.valueOf(msg.memberId())
                )
        );
        stream.add(args);
    }
}
