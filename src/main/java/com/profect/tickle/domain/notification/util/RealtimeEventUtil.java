package com.profect.tickle.domain.notification.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RealtimeEventUtil {

    /**
     * eventId와 lastEventId가 숫자 문자열(에폭 밀리초)이라고 가정하고
     * eventId가 더 최신인지 판별합니다.
     */
    public static boolean isAfterEventId(String eventId, String lastEventId) {
        if (eventId == null || lastEventId == null) return false;
        try {
            return Long.parseLong(eventId) > Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            log.warn("Invalid event id(s): eventId={}, lastEventId={}", eventId, lastEventId);
            return false;
        }
    }
}
