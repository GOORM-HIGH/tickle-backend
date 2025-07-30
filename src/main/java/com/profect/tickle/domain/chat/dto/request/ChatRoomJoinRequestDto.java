package com.profect.tickle.domain.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomJoinRequestDto {
    // 채팅방 참여는 JWT에서 memberId를 가져오고,
    // PathVariable에서 roomId를 가져오므로
    // RequestBody는 비어있어도 됨

    // 필요시 추가 정보를 받을 수 있음
    private String message; // 참여 메시지 (선택사항)
}
