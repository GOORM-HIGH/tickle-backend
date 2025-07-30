package com.profect.tickle.domain.chat.dto.response;

import com.profect.tickle.domain.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponseDto {

    private Long chatRoomId;
    private Long performanceId;
    private String name;
    private Boolean status;
    private Short maxParticipants;
    private Instant createdAt;
    private Instant updatedAt;

    // 추가 정보 (복잡한 쿼리로 가져올 데이터)
    private Integer participantCount;
    private Boolean isParticipant;
    private Integer unreadCount;
    private ChatMessageResponseDto lastMessage;

    // Entity → DTO 변환
    public static ChatRoomResponseDto fromEntity(ChatRoom chatRoom) {
        return ChatRoomResponseDto.builder()
                .chatRoomId(chatRoom.getId())
                .performanceId(chatRoom.getPerformanceId())
                .name(chatRoom.getName())
                .status(chatRoom.getStatus())
                .maxParticipants(chatRoom.getMaxParticipants())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }

    // 추가 정보를 포함한 완전한 DTO 생성
    public static ChatRoomResponseDto fromEntityWithDetails(
            ChatRoom chatRoom,
            Integer participantCount,
            Boolean isParticipant,
            Integer unreadCount,
            ChatMessageResponseDto lastMessage) {

        return ChatRoomResponseDto.builder()
                .chatRoomId(chatRoom.getId())
                .performanceId(chatRoom.getPerformanceId())
                .name(chatRoom.getName())
                .status(chatRoom.getStatus())
                .maxParticipants(chatRoom.getMaxParticipants())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .participantCount(participantCount)
                .isParticipant(isParticipant)
                .unreadCount(unreadCount)
                .lastMessage(lastMessage)
                .build();
    }
}
