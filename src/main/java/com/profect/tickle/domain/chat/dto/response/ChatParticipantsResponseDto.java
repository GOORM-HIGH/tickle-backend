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

    private Long chatParticipantsId;
    private Long chatRoomId;
    private Long memberId;
    private Instant chatParticipantsJoinedAt;
    private Boolean chatParticipantsStatus;
    private Instant chatParticipantsLastReadAt;
    private Long chatParticipantsLastReadMessageId;

    // 추가 정보 (복잡한 쿼리로 가져올 데이터)
    private String memberNickname;
    private Integer unreadMessageCount;
    private Boolean isOnline;

    // Entity → DTO 변환 (기본)
    public static ChatParticipantsResponseDto fromEntity(ChatParticipants participant) {
        return ChatParticipantsResponseDto.builder()
                .chatParticipantsId(participant.getId())
                .chatRoomId(participant.getChatRoom().getId())
                .memberId(participant.getMember().getId())
                .chatParticipantsJoinedAt(participant.getJoinedAt())
                .chatParticipantsStatus(participant.getStatus())
                .chatParticipantsLastReadAt(participant.getLastReadAt())
                .chatParticipantsLastReadMessageId(participant.getLastReadMessageId())
                .build();
    }

    // 추가 정보를 포함한 완전한 DTO 생성
    public static ChatParticipantsResponseDto fromEntityWithDetails(
            ChatParticipants participant,
            String memberNickname,
            Integer unreadMessageCount,
            Boolean isOnline) {

        return ChatParticipantsResponseDto.builder()
                .chatParticipantsId(participant.getId())
                .chatRoomId(participant.getChatRoom().getId())
                .memberId(participant.getMember().getId())
                .chatParticipantsJoinedAt(participant.getJoinedAt())
                .chatParticipantsStatus(participant.getStatus())
                .chatParticipantsLastReadAt(participant.getLastReadAt())
                .chatParticipantsLastReadMessageId(participant.getLastReadMessageId())
                .memberNickname(memberNickname)
                .unreadMessageCount(unreadMessageCount)
                .isOnline(isOnline)
                .build();
    }
}
