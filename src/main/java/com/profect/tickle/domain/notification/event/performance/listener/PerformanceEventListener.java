package com.profect.tickle.domain.notification.event.performance.listener;

import com.profect.tickle.domain.member.dto.response.MemberResponseDto;
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
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationServiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RealtimeSender realtimeSender;
    private final MemberService memberService;

    // 제휴 업체 공연 게시 시 알림 전송 (브로드캐스트)
    @EventListener
    public void handlePartnerPerformancePublished(PartnerPerformancePublishedEvent event) {
        log.info("[이벤트 감지] 제휴 공연 게시: \"{}\"", event.performance().title());

        // 1) 템플릿 조회
        NotificationTemplate template = notificationTemplateService
                .getNotificationTemplateById(NotificationKind.PARTNER_PERFORMANCE_PUBLISHED.getId());

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
        List<MemberResponseDto> memberList = memberService.findMemberListByDeletedAtIsNull(true);
        memberList.forEach(member -> notificationService.saveNotification(member.getEmail(), template, subject, content, now));

        // 4) SSE 브로드캐스트
        NotificationEnvelope<Void> payload = new NotificationEnvelope<>(
                NotificationKind.PARTNER_PERFORMANCE_PUBLISHED,
                subject,
                content,
                now,
                link,
                null
        );

        realtimeSender.sendAll(payload);
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
                mailSender.sendText(new MailCreateServiceRequestDto(reservation.getMemberEmail(), subject, content));

                // 실시간 알림 전송
                NotificationEnvelope<Void> payload = new NotificationEnvelope<>(
                        NotificationKind.PERFORMANCE_MODIFIED,
                        subject,
                        content,
                        now,
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
