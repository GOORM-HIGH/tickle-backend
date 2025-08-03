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
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.domain.reservation.entity.Seat;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.Status;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MemberService memberService;
    private final StatusService statusService;
    private final NotificationTemplateService notificationTemplateService;
    private final PerformanceService performanceService;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateMapper notificationTemplateMapper;
    private final MailService mailService; // ✅ 메일 서비스 추가

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();
    private final Long TIME_OUT = 60 * 60 * 1000L;

    // 최신 알림 조회
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecentNotificationListByMemberId(Long memberId) {
        return notificationMapper.getRecentNotificationListByMemberId(memberId);
    }

    // 알림 읽은 처리
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if (notification != null && !notification.getReceivedMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        Status isReadStatus = statusService.getReadStatusForNotification();
        notification.markAsRead(isReadStatus);
    }

    // SSE 연결
    public SseEmitter sseConnect(String lastEventId) {
        String emitterId = SecurityUtil.getSignInMemberEmail();
        SseEmitter emitter = new SseEmitter(TIME_OUT);
        emitterMap.put(emitterId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(emitterId));
        emitter.onTimeout(() -> emitterMap.remove(emitterId));

        try {
            emitter.send(SseEmitter.event()
                    .name("sse connect")
                    .data("connected"));
        } catch (Exception e) {
            emitterMap.remove(emitterId);
        }

        if (!lastEventId.isEmpty()) {
            resendMissedEvents(emitter, lastEventId);
        }
        return emitter;
    }

    // 알림 전송
    public void sendNotification(Long memberId, String message) {
        emitterMap.forEach((id, emitter) -> {
            if (id.startsWith(SecurityUtil.getSignInMemberEmail())) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(message)
                            .id(String.valueOf(System.currentTimeMillis())));
                } catch (Exception e) {
                    emitterMap.remove(id);
                }
            }
        });
    }

    // 유실 이벤트 재전송
    private void resendMissedEvents(SseEmitter emitter, String lastEventId) {
        eventCache.forEach((eventId, event) -> {
            if (Long.parseLong(eventId) > Long.parseLong(lastEventId)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(event)
                            .id(eventId));
                } catch (Exception e) {
                    // 실패 시 무시
                }
            }
        });
    }

    // 쿠폰 만료 임박 알림
    @Transactional
    public void sendCouponAlmostExpiredNotification(String memberEmail, String couponName, LocalDate expiryDate) {
        NotificationTemplate template = notificationTemplateService.getNotificationTemplateById(
                NotificationTemplateId.COUPON_ALMOST_EXPIRED.getId());
        Instant now = Instant.now();

        String title = String.format(template.getTitle(), couponName);
        String message = String.format(template.getContent(), couponName, expiryDate.toString(), now);

        // SSE 전송
        sendNotificationToClient(NotificationSseResponseDto.builder().title(title).message(message).build());

        // 메일 발송
        mailService.sendSimpleMailMessage(memberEmail, title, message);

        // DB 저장
        saveNotificationWithMemberEmail(memberEmail, template, now);
    }

    private void saveNotificationWithMemberEmail(
            String memberEmail,
            NotificationTemplate template,
            Instant createdAt
    ) {
        Member member = memberService.getMemberByEmail(memberEmail);
        notificationRepository.save(Notification.builder()
                .receivedMember(member)
                .template(template)
                .status(statusService.getReadYetStatusForNotification())
                .createdAt(createdAt)
                .build());
    }

    // 예매 성공 알림
    public void sendReservationSuccessNotification(ReservationSuccessEvent event) {
        NotificationTemplate template = getTemplate(NotificationTemplateId.RESERVATION_SUCCESS);
        Performance performance = event.reservation().getPerformance();
        List<Seat> seatList = getSeatList(event.reservation().getId());
        Member receiver = event.reservation().getMember();
        Instant now = Instant.now();

        String title = formatTitle(template, performance);
        String message = formatMessage(template, performance, seatList, now);

        // SSE 전송
        sendNotificationToClient(NotificationSseResponseDto.builder().title(title).message(message).build());

        // 메일 발송
        mailService.sendSimpleMailMessage(receiver.getEmail(), title, message);

        // DB 저장
        saveNotification(receiver, template, now);
    }

    // 예매한 공연 내용 수정(삭제)
    public void sendPerformanceModifiedNotification(PerformanceModifiedEvent event) {
        NotificationTemplate template = getTemplate(NotificationTemplateId.PERFORMANCE_MODIFIED);
        Performance performance = event.reservation().getPerformance();
        List<Seat> seatList = getSeatList(event.reservation().getId());
        Member receiver = event.reservation().getMember();
        Instant now = Instant.now();

        String title = formatTitle(template, performance);
        String message = formatMessage(template, performance, seatList, now);

        // SSE 전송
        sendNotificationToClient(NotificationSseResponseDto.builder().title(title).message(message).build());

        // 메일 발송
        mailService.sendSimpleMailMessage(receiver.getEmail(), title, message);

        // DB 저장
        saveNotification(receiver, template, now);
    }


    // 템플릿 조회
    private NotificationTemplate getTemplate(NotificationTemplateId templateId) {
        return notificationTemplateService.getNotificationTemplateById(templateId.getId());
    }

    // 자리 리스트 조회
    private List<Seat> getSeatList(Long reservationId) {
        // TODO: seatService 주입받아 실제 구현
        return null;
    }

    // 제목 포맷팅
    private String formatTitle(NotificationTemplate template, Performance performance) {
        return String.format(template.getTitle(), performance.getTitle());
    }

    // 메시지 포맷팅
    private String formatMessage(NotificationTemplate template, Performance performance, List<Seat> seats, Instant now) {
        String seatCodeString = String.join("\n", seats.stream().map(Seat::getSeatCode).toList());
        return String.format(template.getContent(),
                performance.getDate(),
                performance.getHall().getAddress(),
                seatCodeString,
                now);
    }

    // 알림 저장
    private void saveNotification(Member receiver, NotificationTemplate template, Instant createdAt) {
        Status unreadStatus = statusService.getReadYetStatusForNotification();
        notificationRepository.save(Notification.builder()
                .receivedMember(receiver)
                .template(template)
                .status(unreadStatus)
                .createdAt(createdAt)
                .build());
    }

    // SSE 전송
    public void sendNotificationToClient(Object data) {
        String emitterId = SecurityUtil.getSignInMemberEmail();
        if (emitterMap.containsKey(emitterId)) {
            SseEmitter emitter = emitterMap.get(emitterId);
            try {
                emitter.send(SseEmitter.event()
                        .name(emitterId)
                        .data(data, MediaType.APPLICATION_JSON)
                        .id(String.valueOf(System.currentTimeMillis())));
            } catch (IOException e) {
                emitterMap.remove(emitterId);
                emitter.completeWithError(e);
            }
        }
    }
}
