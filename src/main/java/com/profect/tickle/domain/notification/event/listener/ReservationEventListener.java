package com.profect.tickle.domain.notification.event.listener;

import com.profect.tickle.domain.notification.event.event.ReservationSuccessEvent;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.global.security.util.SecurityUtil;
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

    @Async
    @EventListener
    public void handleReservationSuccess(ReservationSuccessEvent event) {
        log.info("예매 성공 이벤트 감지: memberId={}, 공연명={}, 예매 Id={}", SecurityUtil.getSignInMemberId(), event.reservation().getPerformance().getTitle(), event.reservation().getId());
        notificationService.sendReservationSuccessNotification(event);

        /* TODO: 이벤트 발행
         *  bean으로 private final ApplicationEventPublisher eventPublisher; 를 주입
         *  eventPublisher.publishEvent(new ReservationSuccessEvent(reservation));
         *  마지막에 넣기
         * */
    }

    @Async
    @EventListener
    public void handlePerformanceModified(ReservationSuccessEvent event) {
        log.info("공연 내용 수정 이벤트 감지: memberId={}, performanceTitle={}", SecurityUtil.getSignInMemberId(), event.reservation().getPerformance().getTitle());
        notificationService.sendReservationSuccessNotification(event);

        /* TODO: 이벤트 발행
         *  bean으로 private final ApplicationEventPublisher eventPublisher; 를 주입
         *  eventPublisher.publishEvent(new ReservationSuccessEvent(reservation));
         *  마지막에 넣기
         * */
    }
}
