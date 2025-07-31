package com.profect.tickle.domain.chat.dto.websocket;

import com.profect.tickle.domain.chat.entity.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageRequestDto {

    private String type;  // "MESSAGE", "JOIN", "LEAVE", "TYPING"
    private Long chatRoomId;
    private Long senderId;
    private String senderNickname;
    private ChatMessageType messageType;
    private String content;
    private String filePath;
    private String fileName;
    private Integer fileSize;
    private String fileType;
}
