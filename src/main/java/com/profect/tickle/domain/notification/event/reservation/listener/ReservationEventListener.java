package com.profect.tickle.domain.notification.event.reservation.listener;

import com.profect.tickle.domain.notification.event.reservation.event.PerformanceModifiedEvent;
import com.profect.tickle.domain.notification.event.reservation.event.ReservationSuccessEvent;
import com.profect.tickle.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventListener {

    private final NotificationService notificationService;

    // 예매 성공 시 알림 전송
    @Async
    @EventListener
    public void handleReservationSuccess(ReservationSuccessEvent event) {
        log.info("예매 성공 이벤트 감지: memberId={}, 공연명={}, 예매 Id={}",
                event.member().getEmail(),
                event.performance().getTitle(),
                event.reservation().getId()
        );

//        notificationService.sendReservationSuccessNotification(event);
    }

    // 공연 정보 수정 시 알림 전송
    @Async
    @EventListener
    public void handlePerformanceModified(PerformanceModifiedEvent event) {
        log.info("공연 내용 수정 이벤트 감지: {}", event.performance().getTitle());

//        notificationService.sendPerformanceModifiedNotification(event);
    }
}
