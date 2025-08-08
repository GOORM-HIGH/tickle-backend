package com.profect.tickle.domain.event.scheduler;

import com.profect.tickle.domain.event.dto.response.ExpiringSoonCouponResponseDto;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final EventService eventService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 만료 임박 쿠폰 이벤트 발행 로직 (공통)
     */
    public void publishExpiringSoonCoupons() {
        List<ExpiringSoonCouponResponseDto> expiringCouponList = eventService.getCouponsExpiringWithinOneDay();

        log.info("임박 쿠폰 조회 스케쥴러 실행!!!");
        log.info("만료 임박 쿠폰 {}건 조회됨", expiringCouponList.size());
        expiringCouponList.forEach(data -> {
            log.info("메일 전송: {}",data.memberEmail());
        });

        expiringCouponList.forEach(coupon ->{
                eventPublisher.publishEvent(
                        new CouponAlmostExpiredEvent(
                                coupon.memberEmail(),
                                coupon.couponName(),
                                coupon.expiryDate()
                        )
                );
                log.info("이벤트 생성");
        }
        );
    }

    /**
     * 애플리케이션 시작 시 1회 실행
     */
    @PostConstruct
    public void runOnStartup() {
        publishExpiringSoonCoupons();
    }

    /**
     * 매일 자정에 실행 (00:00)
     */
    // TODO: 테스트 30초마다 실행
    @Scheduled(cron = "0/30 * * * * *", zone = "Asia/Seoul")
    public void runDaily() {
        publishExpiringSoonCoupons();
    }
}
