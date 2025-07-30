package com.profect.tickle.domain.chat.dto.response;

import com.profect.tickle.domain.chat.dto.common.PaginationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageListResponseDto {

    private List<ChatMessageResponseDto> messages;
    private PaginationDto pagination;

    public static ChatMessageListResponseDto of(
            List<ChatMessageResponseDto> messages,
            PaginationDto pagination) {

        return ChatMessageListResponseDto.builder()
                .messages(messages)
                .pagination(pagination)
                .build();
    }
}
