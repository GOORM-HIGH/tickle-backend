package com.profect.tickle.global.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketStats {
    private int totalSessions;    // 전체 활성 세션 수
    private int totalUsers;       // 전체 온라인 사용자 수
    private int totalRooms;       // 활성 채팅방 수
}
