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
    private Boolean isDeleted;  // 삭제 상태 추가

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

    // DTO 생성 컨텍스트 클래스
    public static class ChatMessageContext {
        private final Chat chat;
        private final String senderNickname;
        private final Boolean isMyMessage;

        public ChatMessageContext(Chat chat, String senderNickname, Boolean isMyMessage) {
            this.chat = chat;
            this.senderNickname = senderNickname;
            this.isMyMessage = isMyMessage;
        }

        public Chat getChat() { return chat; }
        public String getSenderNickname() { return senderNickname; }
        public Boolean getIsMyMessage() { return isMyMessage; }
    }

    // 컨텍스트를 사용한 DTO 생성 (개선된 방식)
    public static ChatMessageResponseDto fromContext(ChatMessageContext context) {
        Chat chat = context.getChat();
        
        return ChatMessageResponseDto.builder()
                .id(chat.getId())
                .chatRoomId(chat.getChatRoomId())
                .memberId(chat.getMember() != null ? chat.getMember().getId() : null)
                .messageType(chat.getMessageType())
                .content(chat.getIsDeleted() ? "삭제된 메시지입니다" : chat.getContent())
                .createdAt(chat.getCreatedAt())
                .senderStatus(chat.getSenderStatus())
                .isDeleted(chat.getIsDeleted())
                .filePath(chat.getFilePath())
                .fileName(chat.getFileName())
                .fileSize(chat.getFileSize())
                .fileType(chat.getFileType())
                .senderNickname(chat.getSenderStatus() ? context.getSenderNickname() : "탈퇴한 회원입니다")
                .isMyMessage(context.getIsMyMessage())
                .build();
    }

    // 기존 메서드 (하위 호환성을 위해 유지)
    public static ChatMessageResponseDto fromEntityWithDetails(
            Chat chat,
            String senderNickname,
            Boolean isMyMessage) {
        
        ChatMessageContext context = new ChatMessageContext(chat, senderNickname, isMyMessage);
        return fromContext(context);
    }
}
