package com.profect.tickle.domain.event.stream;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StreamInitializer {

    public static final String STREAM_KEY = "stream:ticket-events";
    public static final String GROUP      = "ticket-workers";

    private final RedissonClient redisson;
    private final TypedJsonJacksonCodec streamFieldMapCodec;

    @PostConstruct
    public void init() {
        RStream<String, Object> stream = redisson.getStream(STREAM_KEY, streamFieldMapCodec);

        // 이미 그룹이 있으면 패스
        boolean hasGroup = stream.listGroups().stream()
                .anyMatch(g -> GROUP.equals(g.getName()));
        if (hasGroup) return;

        // 1) 스트림이 없을 수도 있으니 임시 메시지로 생성해둠
        StreamMessageId tmpId = stream.add(
                org.redisson.api.stream.StreamAddArgs.entries(
                        java.util.Collections.singletonMap("_bootstrap", "1")
                )
        );

        // 2) XGROUP CREATE ... `$`
        stream.createGroup(GROUP, StreamMessageId.NEWEST); // '$' 의미

        // 3) 임시 메시지는 선택 삭제
        stream.remove(tmpId);
    }
}