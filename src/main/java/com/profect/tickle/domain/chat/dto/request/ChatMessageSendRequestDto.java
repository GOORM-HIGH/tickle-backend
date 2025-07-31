package com.profect.tickle.domain.chat.dto.request;

import com.profect.tickle.domain.chat.entity.ChatMessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendRequestDto {

    @NotNull(message = "메시지 타입은 필수입니다")
    private ChatMessageType messageType;

    @Size(max = 255, message = "메시지 내용은 255자 이하여야 합니다")
    private String content;

    // 파일 관련 정보 (파일 메시지인 경우)
    private String filePath;
    private String fileName;
    private Integer fileSize;
    private String fileType;
}
