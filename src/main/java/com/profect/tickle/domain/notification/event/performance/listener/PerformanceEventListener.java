package com.profect.tickle.domain.notification.event.performance.listener;

import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.entity.NotificationKind;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.event.performance.event.PartnerPerformancePublishedEvent;
import com.profect.tickle.domain.notification.event.performance.event.PerformanceModifiedEvent;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.domain.notification.service.mail.MailSender;
import com.profect.tickle.domain.notification.service.realtime.producer.MessageProducer;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceEventListener {

    // utils
    private final Clock clock;

    // services
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final MailSender mailSender;
    @Value("#{@notificationStreamKey}")
    private String notificationStreamKey;
    private final MessageProducer redisNotificationProducer;
    private final MemberService memberService;

    // 제휴 업체 공연 게시 시 알림 전송 (브로드캐스트)
    @EventListener
    public void handlePartnerPerformancePublished(PartnerPerformancePublishedEvent event) throws Exception {
        log.info("[이벤트 감지] 제휴 공연 게시: \"{}\"", event.performance().title());

        // 1) 템플릿 조회
        NotificationTemplate template = notificationTemplateService.getNotificationTemplateById(NotificationKind.PARTNER_PERFORMANCE_PUBLISHED.getId());

        // 2) 내용 생성
        String link = "https://tickle.kr/performances/" + event.performance().id();
        Instant now = clock.instant();

        String lineTitle = "[공연제목]: " + event.performance().title();
        String lineLink = "[링크]: " + link;
        String lineStartDate = "[예매가능일]: " + event.performance().startDate();
        String linePerformanceDateAndTime = "[공연일시]: " + event.performance().performanceDateAndTime();

        String contentBody = String.join("\n", lineTitle, lineLink, lineStartDate, linePerformanceDateAndTime);

        // 2-2) 내용 등록
        String subject = String.format(template.getTitle(), event.performance().title());
        String content = String.format(template.getContent(), contentBody);

        // 3) 회원 알림 저장
        List<Long> memberIdList = memberService.findMemberListByDeletedAtIsNull(true);
        notificationService.saveAll(memberIdList, template.getId(), subject, content, now);

        // 4) SSE 브로드캐스트
        NotificationEnvelope<Void> payload = new NotificationEnvelope<>(NotificationKind.PARTNER_PERFORMANCE_PUBLISHED, null, subject, content, now, link, null);
        redisNotificationProducer.produce(notificationStreamKey, payload);
    }

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
                Instant now = clock.instant();

                String subject = String.format(template.getTitle(), event.performance().title());
                String content = String.format(template.getContent(), newContent);

                // 알림 저장
                notificationService.saveNotification(reservation.getMemberEmail(), template, subject, content, now);

                // 메일 전송
                try {
                    mailSender.sendText(new MailCreateServiceRequestDto(reservation.getMemberEmail(), subject, content));
                    log.info("메일 전송 성공");
                } catch (Exception e) {
                    log.warn("메일 전송 실패 - 다른 알림 채널은 정상 처리: {}", e.getMessage());
                }

                // 실시간 알림 전송
                NotificationEnvelope<Void> payload = new NotificationEnvelope<>(
                        NotificationKind.PERFORMANCE_MODIFIED,
                        reservation.getMemberId(),
                        subject,
                        content,
                        now,
                        "/performances/" + event.performance().id(),
                        null
                );
                redisNotificationProducer.produce(notificationStreamKey, payload);

            } catch (Exception ex) {
                log.warn("공연 수정 알림 전송 실패: memberId={}, err={}",
                        reservation.getMemberId(), ex.toString());
            }
        }
    }
}
