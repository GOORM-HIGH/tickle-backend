package com.profect.tickle.domain.notification.util.constant;

public class NotificationSchedulerConstants {

    // 인스턴스화 방지
    private NotificationSchedulerConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 복구 임계 시간 (분)
    public static final long RECOVERY_THRESHOLD_MINUTES = 5;

    // 복구 배치 최대 크기
    public static final int MAX_RECOVERY_BATCH_SIZE = 50;

    // 메시지 폐기 임계 시간 (1440분 == 24시간)
    public static final long DISCARD_THRESHOLD_MINUTES = 1440;
}
