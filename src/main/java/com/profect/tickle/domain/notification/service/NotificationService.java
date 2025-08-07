package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.dto.response.NotificationSseResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.entity.NotificationTemplateId;
import com.profect.tickle.domain.notification.event.reservation.event.PerformanceModifiedEvent;
import com.profect.tickle.domain.notification.event.reservation.event.ReservationSuccessEvent;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.mapper.NotificationTemplateMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Long TIME_OUT = 60 * 60 * 1000L;

    private final MemberService memberService;
    private final StatusService statusService;
    private final NotificationTemplateService notificationTemplateService;
    private final PerformanceService performanceService;
    private final MailService mailService;

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final SseRepository sseRepository;

    /**
     * 최신 알림 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecentNotificationListByMemberId(Long memberId, int limit) {
        return notificationMapper.getRecentNotificationListByMemberId(memberId, limit);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceivedMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }
        notification.markAsRead(statusService.getReadStatusForNotification());
    }

    /**
     * SSE 연결
     */
    public SseEmitter sseConnect(String lastEventId) {
        String emitterId = SecurityUtil.getSignInMemberEmail();
        SseEmitter emitter = new SseEmitter(TIME_OUT);
        sseRepository.save(emitterId, emitter);

        emitter.onCompletion(() -> sseRepository.deleteById(emitterId));
        emitter.onTimeout(() -> sseRepository.deleteById(emitterId));

        try {
            emitter.send(SseEmitter.event().name("sse connect").data("connected"));
        } catch (IOException e) {
            sseRepository.deleteById(emitterId);
        }

        if (!lastEventId.isEmpty()) {
            resendMissedSseEvents(emitter, lastEventId);
        }
        return emitter;
    }

    /**
     * 알림 전송
     */
    public void sendSseNotification(String id, String message) {
        SseEmitter emitter = sseRepository.get(id);
        if (emitter == null) return;

        String eventId = String.valueOf(System.currentTimeMillis());
        try {
            emitter.send(SseEmitter.event().name("notification").data(message, MediaType.APPLICATION_JSON).id(eventId));
            sseRepository.saveEvent(eventId, message);
        } catch (IOException e) {
            sseRepository.deleteById(id);
        }
    }

    /**
     * 유실 이벤트 재전송
     */
    private void resendMissedSseEvents(SseEmitter emitter, String lastEventId) {
        sseRepository.getEventCache().forEach((eventId, event) -> {
            if (Long.parseLong(eventId) > Long.parseLong(lastEventId)) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(event).id(eventId));
                } catch (IOException ignored) {
                }
            }
        });
    }

    /**
     * 쿠폰 만료 임박 알림
     */
    @Transactional
    public void sendCouponAlmostExpiredNotification(String memberEmail, String couponName, LocalDate expiryDate) {
        NotificationTemplate template = getTemplate(NotificationTemplateId.COUPON_ALMOST_EXPIRED);
        Instant now = Instant.now();

        String title = String.format(template.getTitle(), couponName);
        String message = String.format(template.getContent(), couponName, expiryDate.toString(), now);

        sendSseAndSaveNotification(memberEmail, template, title, message, now);
    }

    /**
     * 예매 성공 알림
     */
    public void sendReservationSuccessNotification(ReservationSuccessEvent event) {
        sendPerformanceNotification(event.reservation().getMember(),
                NotificationTemplateId.RESERVATION_SUCCESS,
                event.reservation().getPerformance(),
                getSeatList(event.reservation().getId()));
    }

    /**
     * 공연 수정 알림
     */
    public void sendPerformanceModifiedNotification(PerformanceModifiedEvent event) {
        sendPerformanceNotification(event.reservation().getMember(),
                NotificationTemplateId.PERFORMANCE_MODIFIED,
                event.reservation().getPerformance(),
                getSeatList(event.reservation().getId()));
    }

    /**
     * 공통 Performance 알림 전송
     */
    private void sendPerformanceNotification(Member receiver, NotificationTemplateId templateId,
                                             Performance performance, List<Seat> seatList) {
        NotificationTemplate template = getTemplate(templateId);
        Instant now = Instant.now();

        String title = String.format(template.getTitle(), performance.getTitle());
        String seatCodes = String.join("\n", seatList.stream().map(Seat::getSeatCode).toList());
        String message = String.format(template.getContent(), performance.getDate(),
                performance.getHall().getAddress(), seatCodes, now);

        sendSseAndSaveNotification(receiver.getEmail(), template, title, message, now);
    }

    /**
     * 알림 전송 + 저장 + 메일 발송
     */
    private void sendSseAndSaveNotification(String memberEmail, NotificationTemplate template,
                                            String title, String message, Instant createdAt) {
        // SSE 전송
        sendSseNotification(memberEmail, String.valueOf(NotificationSseResponseDto.builder().title(title).message(message).build()));
        // 메일 발송
        mailService.sendSimpleMailMessage(memberEmail, title, message);
        // DB 저장
        Member member = memberService.getMemberByEmail(memberEmail);
        notificationRepository.save(Notification.builder()
                .receivedMember(member)
                .template(template)
                .title(title)
                .content(message)
                .status(statusService.getReadYetStatusForNotification())
                .createdAt(createdAt)
                .build());
    }

    /**
     * 템플릿 조회
     */
    private NotificationTemplate getTemplate(NotificationTemplateId templateId) {
        return notificationTemplateService.getNotificationTemplateById(templateId.getId());
    }

    /**
     * 자리 리스트 조회
     */
    private List<Seat> getSeatList(Long reservationId) {
        return List.of(); // TODO: seatService로 실제 구현
    }
}
