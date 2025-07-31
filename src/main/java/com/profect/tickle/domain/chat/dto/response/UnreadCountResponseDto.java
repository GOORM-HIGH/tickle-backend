package com.profect.tickle.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponseDto {

    private Integer unreadCount;
    private Long lastReadMessageId;
    private Instant lastReadAt;

    public static UnreadCountResponseDto of(
            Integer unreadCount,
            Long lastReadMessageId,
            Instant lastReadAt) {

        return UnreadCountResponseDto.builder()
                .unreadCount(unreadCount)
                .lastReadMessageId(lastReadMessageId)
                .lastReadAt(lastReadAt)
                .build();
    }
}
