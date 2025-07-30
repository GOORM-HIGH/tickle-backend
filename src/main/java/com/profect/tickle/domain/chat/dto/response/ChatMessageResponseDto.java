package com.profect.tickle.domain.chat.dto.response;

import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {

    private Long chatId;
    private Long chatRoomId;
    private Long memberId;
    private ChatMessageType chatMessageType;
    private String chatContent;
    private Instant chatCreatedAt;
    private Boolean chatSenderStatus;

    // 파일 관련 정보
    private String chatFilePath;
    private String chatFileName;
    private Integer chatFileSize;
    private String chatFileType;

    // 추가 정보
    private String senderNickname;
    private Boolean isMyMessage;

    // Entity → DTO 변환 (기본)
    public static ChatMessageResponseDto fromEntity(Chat chat) {
        return ChatMessageResponseDto.builder()
                .chatId(chat.getId())
                .chatRoomId(chat.getChatRoomId())
                .memberId(chat.getMember() != null ? chat.getMember().getId() : null)
                .chatMessageType(chat.getMessageType())
                .chatContent(chat.getContent())
                .chatCreatedAt(chat.getCreatedAt())
                .chatSenderStatus(chat.getSenderStatus())
                .chatFilePath(chat.getFilePath())
                .chatFileName(chat.getFileName())
                .chatFileSize(chat.getFileSize())
                .chatFileType(chat.getFileType())
                .build();
    }

    // 추가 정보를 포함한 완전한 DTO 생성
    public static ChatMessageResponseDto fromEntityWithDetails(
            Chat chat,
            String senderNickname,
            Boolean isMyMessage) {

        return ChatMessageResponseDto.builder()
                .chatId(chat.getId())
                .chatRoomId(chat.getChatRoomId())
                .memberId(chat.getMember() != null ? chat.getMember().getId() : null)
                .chatMessageType(chat.getMessageType())
                .chatContent(chat.getSenderStatus() ? chat.getContent() : "삭제된 메시지입니다")
                .chatCreatedAt(chat.getCreatedAt())
                .chatSenderStatus(chat.getSenderStatus())
                .chatFilePath(chat.getFilePath())
                .chatFileName(chat.getFileName())
                .chatFileSize(chat.getFileSize())
                .chatFileType(chat.getFileType())
                .senderNickname(chat.getSenderStatus() ? senderNickname : "탈퇴한 회원입니다")
                .isMyMessage(isMyMessage)
                .build();
    }
}
