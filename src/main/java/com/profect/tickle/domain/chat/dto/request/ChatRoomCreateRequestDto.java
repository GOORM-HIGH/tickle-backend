package com.profect.tickle.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequestDto {

    @NotNull(message = "공연 ID는 필수입니다")
    private Long performanceId;

    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(max = 20, message = "채팅방 이름은 20자 이하여야 합니다")
    private String roomName;

    private Short maxParticipants = 100; // 기본값 100명
}
