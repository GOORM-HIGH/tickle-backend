package com.profect.tickle.domain.chat.dto.response;

import com.profect.tickle.domain.chat.entity.ChatParticipants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantsResponseDto {

    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private Instant joinedAt;
    private Boolean status;
    private Instant lastReadAt;
    private Long lastReadMessageId;

    // 추가 정보 (복잡한 쿼리로 가져올 데이터)
    private String memberNickname;
    private Integer unreadMessageCount;
    private Boolean isOnline;

    // Entity → DTO 변환 (기본)
    public static ChatParticipantsResponseDto fromEntity(ChatParticipants participant) {
        return ChatParticipantsResponseDto.builder()
                .id(participant.getId())
                .chatRoomId(participant.getChatRoom().getId())
                .memberId(participant.getMember().getId())
                .joinedAt(participant.getJoinedAt())
                .status(participant.getStatus())
                .lastReadAt(participant.getLastReadAt())
                .lastReadMessageId(participant.getLastReadMessageId())
                .build();
    }

    // 추가 정보를 포함한 완전한 DTO 생성
    public static ChatParticipantsResponseDto fromEntityWithDetails(
            ChatParticipants participant,
            String memberNickname,
            Integer unreadMessageCount,
            Boolean isOnline) {

        return ChatParticipantsResponseDto.builder()
                .id(participant.getId())
                .chatRoomId(participant.getChatRoom().getId())
                .memberId(participant.getMember().getId())
                .joinedAt(participant.getJoinedAt())
                .status(participant.getStatus())
                .lastReadAt(participant.getLastReadAt())
                .lastReadMessageId(participant.getLastReadMessageId())
                .memberNickname(memberNickname)
                .unreadMessageCount(unreadMessageCount)
                .isOnline(isOnline)
                .build();
    }
}
