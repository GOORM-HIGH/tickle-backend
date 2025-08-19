package com.profect.tickle.global.status.service;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatusProvider {

    private final StatusRepository statusRepository;
    private final Map<Long, Status> cache = new ConcurrentHashMap<>();

    @PostConstruct
    private void initCache() {
        try {
            List<Status> allStatuses = statusRepository.findAll();
            allStatuses.forEach(status -> cache.put(status.getId(), status));
            log.info("Status cache initialized with {} statuses", cache.size());
        } catch (Exception e) {
            log.error("Failed to initialize status cache", e);
            throw new BusinessException(ErrorCode.STATUS_CACHE_INIT_FAILED);
        }
    }

    public Status provide(Long statusId) {
        if (statusId == null) {
            throw new BusinessException(ErrorCode.STATUS_ID_NULL);
        }

        Status status = cache.get(statusId);
        if (status == null) {
            status = statusRepository.findById(statusId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
            cache.put(statusId, status);
            log.debug("Status added to cache: {}", status);
        }
        return status;
    }

//    // ================= 공연 상태 =================
//    public Status providePerformanceScheduled() {
//        return provide(StatusIds.Performance.SCHEDULED);
//    }
//
//    public Status providePerformanceInProgress() {
//        return provide(StatusIds.Performance.IN_PROGRESS);
//    }
//
//    public Status providePerformanceCompleted() {
//        return provide(StatusIds.Performance.COMPLETED);
//    }
//
//    // ================= 이벤트 상태 =================
//    public Status provideEventScheduled() {
//        return provide(StatusIds.Event.SCHEDULED);
//    }
//
//    public Status provideEventInProgress() {
//        return provide(StatusIds.Event.IN_PROGRESS);
//    }
//
//    public Status provideEventCompleted() {
//        return provide(StatusIds.Event.COMPLETED);
//    }
//
//    // ================= 알림 상태 =================
//    public Status provideNotificationUnread() {
//        return provide(StatusIds.Notification.UNREAD);
//    }
//
//    public Status provideNotificationRead() {
//        return provide(StatusIds.Notification.READ);
//    }
//
//    // ================= 예매 상태 =================
//    public Status provideReservationPaid() {
//        return provide(StatusIds.Reservation.PAID);
//    }
//
//    public Status provideReservationCancelled() {
//        return provide(StatusIds.Reservation.CANCELLED);
//    }
//
//    // ================= 좌석 상태 =================
//    public Status provideSeatAvailable() {
//        return provide(StatusIds.Seat.AVAILABLE);
//    }
//
//    public Status provideSeatPreempted() {
//        return provide(StatusIds.Seat.PREEMPTED);
//    }
//
//    public Status provideSeatReserved() {
//        return provide(StatusIds.Seat.RESERVED);
//    }
//
//    // ================= 정산 상태 =================
//    public Status provideSettlementScheduled() {
//        return provide(StatusIds.Settlement.SCHEDULED);
//    }
//
//    public Status provideSettlementCompleted() {
//        return provide(StatusIds.Settlement.COMPLETED);
//    }
//
//    public Status provideSettlementRefundRequested() {
//        return provide(StatusIds.Settlement.REFUND_REQUESTED);
//    }
//
//    // ================= 쿠폰 상태 =================
//    public Status provideCouponAvailable() {
//        return provide(StatusIds.Coupon.AVAILABLE);
//    }
//
//    public Status provideCouponUsed() {
//        return provide(StatusIds.Coupon.USED);
//    }
}
