package com.profect.tickle.domain.notification.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RealtimeEventUtil {

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
