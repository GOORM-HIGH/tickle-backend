package com.profect.tickle.domain.notification.event.performance.listener;

import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.event.performance.event.PerformanceModifiedEvent;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.domain.notification.service.mail.MailSender;
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceEventListener {

    private final NotificationTemplateService notificationTemplateService;
    private final MailSender mailSender;
    private final RealtimeSender realtimeSender;
    private final Clock clock;

    // 공연 정보 수정 시 알림 전송
    @EventListener
    public void handlePerformanceModified(PerformanceModifiedEvent event) {
        log.info("[이벤트 감지] 공연 수정: \"{}\"", event.performance().title());

        NotificationTemplate template = notificationTemplateService
                .getNotificationTemplateById(NotificationKind.PERFORMANCE_MODIFIED.getId());

        for (ReservationServiceDto reservation : event.reservationList()) {
            try {
                String newTitle = "[공연제목]: " + event.performance().title();
                String newDate = "[일  자]: " + event.performance().performanceDateAndTime();
                String newImg = "[이미지]: " + event.performance().thumbnailUrl();
                String newContent = String.join("\n", newTitle, newDate, newImg);

                String subject = String.format(template.getTitle(), event.performance().title());
                String content = String.format(template.getContent(), newContent);

                // 메일 전송
                mailSender.sendText(new MailCreateServiceRequestDto(reservation.getMemberEmail(), subject, content));

                // 실시간 알림 전송
                NotificationEnvelope<Void> payload = new NotificationEnvelope<>(
                        NotificationKind.PERFORMANCE_MODIFIED,
                        subject,
                        content,
                        Instant.now(clock),
                        "/performances/" + event.performance().id(),
                        null
                );
                realtimeSender.send(reservation.getMemberId(), payload);

            } catch (Exception ex) {
                log.warn("공연 수정 알림 전송 실패: memberId={}, err={}",
                        reservation.getMemberId(), ex.toString());
            }
        }
    }
}
