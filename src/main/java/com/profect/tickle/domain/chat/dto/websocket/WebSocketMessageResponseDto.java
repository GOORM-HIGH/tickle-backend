package com.profect.tickle.domain.chat.dto.websocket;

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
public class WebSocketMessageResponseDto {

    private String type;  // "MESSAGE", "USER_JOIN", "USER_LEAVE", "TYPING", "ERROR", "DELETE"
    private Long messageId;
    private Long chatRoomId;
    private Long senderId;
    private String senderNickname;
    private ChatMessageType messageType;
    private String content;
    private String filePath;
    private String fileName;
    private Integer fileSize;
    private String fileType;
    private Instant createdAt;
    // 🎯 isMyMessage 제거 - 프론트엔드에서 계산
    private Integer onlineCount;  // 현재 온라인 사용자 수
    private String message;  // 시스템 메시지용

    // 메시지 타입 상수
    public static class MessageType {
        public static final String MESSAGE = "MESSAGE";
        public static final String USER_JOIN = "USER_JOIN";
        public static final String USER_LEAVE = "USER_LEAVE";
        public static final String TYPING = "TYPING";
        public static final String ERROR = "ERROR";
        public static final String DELETE = "DELETE";
    }
}
