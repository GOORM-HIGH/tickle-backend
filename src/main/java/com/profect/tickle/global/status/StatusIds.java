package com.profect.tickle.global.status;

/**
 * 상태 ID 상수들을 중앙에서 관리
 * 매직넘버를 방지하고 IDE의 자동완성과 리팩터링 지원
 */
public final class StatusIds {

    private StatusIds() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    // 공연 상태
    public static final class Performance {
        private Performance() {}

        public static final Long SCHEDULED = 1L;      // 공연예정
        public static final Long IN_PROGRESS = 2L;    // 공연진행
        public static final Long COMPLETED = 3L;      // 공연완료
    }

    // 이벤트 상태
    public static final class Event {
        private Event() {}

        public static final Long SCHEDULED = 4L;      // 이벤트 예정
        public static final Long IN_PROGRESS = 5L;    // 이벤트 진행
        public static final Long COMPLETED = 6L;      // 이벤트 완료
    }

    // 알림 상태
    public static final class Notification {
        private Notification() {}

        public static final Long UNREAD = 7L;         // 알림 안읽음
        public static final Long READ = 8L;           // 알림 읽음
    }

    // 예매 상태
    public static final class Reservation {
        private Reservation() {}

        public static final Long PAID = 9L;           // 예매 결제
        public static final Long CANCELLED = 10L;     // 예매 취소
    }

    // 좌석 상태
    public static final class Seat {
        private Seat() {}

        public static final Long AVAILABLE = 11L;     // 좌석 예매 가능
        public static final Long PREEMPTED = 12L;     // 좌석 선점중
        public static final Long RESERVED = 13L;      // 좌석 예매 완료
    }

    // 정산 상태
    public static final class Settlement {
        private Settlement() {}

        public static final Long SCHEDULED = 14L;           // 정산 예정
        public static final Long COMPLETED = 15L;           // 정산 완료
        public static final Long REFUND_REQUESTED = 16L;    // 환불 청구
    }

    // 쿠폰 상태
    public static final class Coupon {
        private Coupon() {}

        public static final Long AVAILABLE = 17L;     // 쿠폰 사용 가능
        public static final Long USED = 18L;          // 쿠폰 사용 완료
    }
}
