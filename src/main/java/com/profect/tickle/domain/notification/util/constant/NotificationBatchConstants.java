package com.profect.tickle.domain.notification.util.constant;

public class NotificationBatchConstants {

    // 인스턴스화 방지
    public NotificationBatchConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // 브로드캐스트 시 청크 크기
    public static final int CHUNK_SIZE = 500;
}
