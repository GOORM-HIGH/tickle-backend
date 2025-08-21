package com.profect.tickle.domain.notification.event.coupon.listener;

import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.event.coupon.event.CouponAlmostExpiredEvent;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.domain.notification.service.mail.MailSender;
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventListener {

    private final NotificationTemplateService notificationTemplateService;
    private final MailSender mailSender;
    private final RealtimeSender realtimeSender;
    private final Clock clock;

    // 쿠폰 만료 임박 이벤트 처리
    @EventListener
    public void handleCouponAlmostExpired(CouponAlmostExpiredEvent event) {
        try {
            log.info("쿠폰 만료 임박 이벤트 감지: memberEmail={}, couponName={}, expiryDate={}",
                    event.memberEmail(), event.couponName(), event.expiryDate());

            // 1) 템플릿 조회
            NotificationTemplate template = notificationTemplateService
                    .getNotificationTemplateById(NotificationKind.COUPON_ALMOST_EXPIRED.getId());

            // 2) 제목/내용 렌더링
            String subject = String.format(template.getTitle(), event.couponName());
            String content = String.format(template.getContent(), event.couponName(), event.expiryDate());

            // 3) 메일 발송
            mailSender.sendText(new MailCreateServiceRequestDto(event.memberEmail(), subject, content));

            // 4) 실시간 통신 페이로드
            NotificationEnvelope<Void> payload = new NotificationEnvelope<>(
                    NotificationKind.COUPON_ALMOST_EXPIRED,
                    subject,
                    content,
                    Instant.now(clock),
                    "/my/coupons",
                    null
            );

            // 5) SSE 전송
            realtimeSender.send(event.memberId(), payload);
        } catch (Exception e) {
            log.error("CouponAlmostExpiredEvent 처리 중 오류", e);
        }
    }
}
