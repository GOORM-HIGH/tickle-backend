package com.profect.tickle.domain.notification.util.constant;

public final class NotificationRedisConstants {

    // 인스턴스화 방지
    private NotificationRedisConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 알림 처리를 위한 기본 Consumer Group
    public static final String CONSUMER_GROUP = "sse-notification-processors";

    // SSE 전송을 담당하는 Consumer 이름
    public static final String SSE_CONSUMER = "sse-sender";

    // 메시지 복구를 담당하는 Consumer 이름
    public static final String RECOVERY_CONSUMER = "recovery-consumer";
}