package com.profect.tickle.domain.notification.scheduler;

import com.profect.tickle.domain.event.dto.response.ExpiringSoonCouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    // utils
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // services
    private final EventService eventService;

    /**
     * 기준일(now)로부터 daysAhead일 이내 만료 쿠폰 이벤트 발행
     */
    public void publishExpiringSoonCouponList(long daysAhead) {
        LocalDate today = LocalDate.now(clock);
        LocalDate untilDate = today.plusDays(daysAhead);

        log.info("임박 쿠폰 조회 스케줄러 실행 - today={}, untilDate={} (zone={})",
                today, untilDate, clock.getZone());

        // EventService 내부는 Clock/Zone 기반으로 [now, untilDate+1d 00:00) 조회하도록 구현
        List<ExpiringSoonCouponResponseDto> list =
                eventService.getCouponListExpiringUntil(untilDate);

        log.info("만료 임박 쿠폰 {}건 조회됨", list.size());

        // 개별 예외 격리
        for (ExpiringSoonCouponResponseDto c : list) {
            try {
                eventPublisher.publishEvent(new CouponAlmostExpiredEvent(
                        c.memberId(), c.memberEmail(), c.couponName(), c.expiryDate()
                ));
                log.debug("이벤트 발행 완료 - email={}, coupon={}", c.memberEmail(), c.couponName());
            } catch (Exception ex) {
                log.warn("이벤트 발행 실패 - email={}, coupon={}, err={}",
                        c.memberEmail(), c.couponName(), ex.toString());
            }
        }
    }

    // 매일 자정에 실행 (00:00)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        publishExpiringSoonCouponList(1L);
    }
}
