package com.profect.tickle.domain.notification.event.reservation.listener;

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

    /**
     * 예매 성공 시 알림 전송
     * 현재는 ReservationSuccessEvent 수신 시 동작.
     * 추후: 예매 도메인 서비스에서 예매 성공 시점에 eventPublisher.publishEvent()로 발행 필요.
     */
    @Async
    @EventListener
    public void handleReservationSuccess(ReservationSuccessEvent event) {
        log.info("예매 성공 이벤트 감지: memberId={}, 공연명={}, 예매 Id={}",
                event.member().getEmail(),
                event.performance().getTitle(),
                event.reservation().getId()
        );

        notificationService.sendReservationSuccessNotification(event);

        /* TODO: 추후 구현
         * - 예매 서비스 (ReservationService) 내부에서 예매 완료 시
         *   ApplicationEventPublisher를 주입받아 publishEvent(new ReservationSuccessEvent(reservation)) 호출
         * - 현재는 리스너만 구현된 상태
         */
    }

    /**
     * 공연 정보 수정 시 알림 전송
     * 현재는 ReservationSuccessEvent를 임시로 수신하지만,
     * 추후 ReservationModifiedEvent로 변경 예정.
     * 공연 수정 시점에서 eventPublisher.publishEvent() 호출 필요.
     */
    @Async
    @EventListener
    public void handlePerformanceModified(ReservationSuccessEvent event) {
        log.info("공연 내용 수정 이벤트 감지: memberId={}, performanceTitle={}",
                event.member().getEmail(),
                event.performance().getTitle()
        );

        notificationService.sendReservationSuccessNotification(event);

        /* TODO: 추후 구현
         * - 공연 수정 시 PerformanceService 또는 ReservationService에서
         *   publishEvent(new ReservationModifiedEvent(reservation)) 발행
         * - 현재는 ReservationSuccessEvent를 임시로 재사용 중
         */
    }
}
