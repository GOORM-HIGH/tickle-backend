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

    private String type;  // "MESSAGE", "USER_JOIN", "USER_LEAVE", "TYPING", "ERROR"
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
    private Boolean isMyMessage;
    private Integer onlineCount;  // 현재 온라인 사용자 수
    private String message;  // 시스템 메시지용
}
