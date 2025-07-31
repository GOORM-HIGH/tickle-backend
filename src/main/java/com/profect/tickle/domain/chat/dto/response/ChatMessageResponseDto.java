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

    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private ChatMessageType messageType;
    private String content;
    private Instant createdAt;
    private Boolean senderStatus;

    // 파일 관련 정보
    private String filePath;
    private String fileName;
    private Integer fileSize;
    private String fileType;

    // 추가 정보
    private String senderNickname;
    private Boolean isMyMessage;

    // Entity → DTO 변환 (기본)
    public static ChatMessageResponseDto fromEntity(Chat chat) {
        return ChatMessageResponseDto.builder()
                .id(chat.getId())
                .chatRoomId(chat.getChatRoomId())
                .memberId(chat.getMember() != null ? chat.getMember().getId() : null)
                .messageType(chat.getMessageType())
                .content(chat.getContent())
                .createdAt(chat.getCreatedAt())
                .senderStatus(chat.getSenderStatus())
                .filePath(chat.getFilePath())
                .fileName(chat.getFileName())
                .fileSize(chat.getFileSize())
                .fileType(chat.getFileType())
                .build();
    }

    // 추가 정보를 포함한 완전한 DTO 생성
    public static ChatMessageResponseDto fromEntityWithDetails(
            Chat chat,
            String senderNickname,
            Boolean isMyMessage) {

        return ChatMessageResponseDto.builder()
                .id(chat.getId())
                .chatRoomId(chat.getChatRoomId())
                .memberId(chat.getMember() != null ? chat.getMember().getId() : null)
                .messageType(chat.getMessageType())
                .content(chat.getSenderStatus() ? chat.getContent() : "삭제된 메시지입니다")
                .createdAt(chat.getCreatedAt())
                .senderStatus(chat.getSenderStatus())
                .filePath(chat.getFilePath())
                .fileName(chat.getFileName())
                .fileSize(chat.getFileSize())
                .fileType(chat.getFileType())
                .senderNickname(chat.getSenderStatus() ? senderNickname : "탈퇴한 회원입니다")
                .isMyMessage(isMyMessage)
                .build();
    }
}
