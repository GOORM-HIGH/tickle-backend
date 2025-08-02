package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.dto.response.NotificationSseResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.event.event.reservation.ReservationSuccessEvent;
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
import com.profect.tickle.global.util.NotificationTemplateId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StatusService statusService;
    private final NotificationTemplateService notificationTemplateService;
    private final PerformanceService performanceService;
//    private final ReservationService TODO: 의존성 주입해야됨. 아직 구현 X

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    // tip: ConcurrentHashMap은 멀티스레드 환경에서 사용하기 좋은 thread-safe한 자료구조이다.
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>(); // 연결된 SSE 관리: SeeEmitter 객체 저장용
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>(); // 유신된 이벤트 캐시: 연결이 끊겼있는 동안 발생하는 이벤트 저장용

    private final Long TIME_OUT = 60 * 60 * 1000L;
    private final NotificationTemplateMapper notificationTemplateMapper;

    // 최신 10건의 알림 조회 메서드
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getRecentNotificationListByMemberId(Long memberId) {
        return notificationMapper.getRecentNotificationListByMemberId(memberId);
    }

    // 알림 읽음 표시 메서드
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if (notification != null && !notification.getReceivedMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        Status isReadStatus = statusService.getReadStatusForNotification();

        notification.markAsRead(isReadStatus); // 수정 및 저장
    }

    /**
     * SSE 연결
     */
    public SseEmitter sseConnect(String lastEventId) {
        // 로그인한 사용자의 이메일을 기준으로 emitterId 생성
        String emitterId = SecurityUtil.getSignInMemberEmail();

        // 기본 타임아웃: 1시간
        SseEmitter emitter = new SseEmitter(TIME_OUT);
        emitterMap.put(emitterId, emitter);

        // 연결 종료 및 타임아웃 시 emitterMap에서 제거
        emitter.onCompletion(() -> emitterMap.remove(emitterId));
        emitter.onTimeout(() -> emitterMap.remove(emitterId));

        // 연결 직후 더미 데이터 전송 (브라우저 연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("sse connect")
                    .data("connected")
            );
        } catch (Exception e) {
            emitterMap.remove(emitterId);
        }

        // 클라이언트가 Last-Event-ID를 보냈다면 유실된 알림 재전송 처리
        if (!lastEventId.isEmpty()) {
            resendMissedEvents(emitter, lastEventId);
        }

        return emitter;
    }

    /**
     * 알림 전송
     */
    public void sendNotification(Long memberId, String message) {
        // 현재 연결된 모든 emitter에게 전송
        emitterMap.forEach((id, emitter) -> {
            if (id.startsWith(SecurityUtil.getSignInMemberEmail())) { // 해당 회원에게만
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

    /**
     * 유실 이벤트 재전송
     */
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

    @Transactional
    public void sendCouponAlmostExpiredNotification(Member member, Coupon coupon, Duration remaining) {
        var template = notificationTemplateService.getNotificationTemplateById(NotificationTemplateId.COUPON_ALMOST_EXPIRED.getId());

        String title = String.format(template.getTitle(), coupon.getName());
        String message = String.format(template.getContent(), coupon.getName(), coupon.getContent(), remaining.toHours());

        // SSE 응답 DTO
        NotificationSseResponseDto sseResponse = NotificationSseResponseDto.builder()
                .title(title)
                .message(message)
                .build();

        // 클라이언트에 전송
        sendNotificationToClient(sseResponse);

        // DB 저장
        notificationRepository.save(Notification.builder()
                .receivedMember(member)
                .template(template)
                .status(statusService.getReadYetStatusForNotification())
                .createdAt(Instant.now())
                .build());
    }


    // 알림을 보내는 메서드
    public void sendReservationSuccessNotification(ReservationSuccessEvent event) {
        // 1. 데이터 조회
        NotificationTemplate template = getTemplate(NotificationTemplateId.RESERVATION_SUCCESS);
        Performance performance = event.reservation().getPerformance();
        List<Seat> seatList = getSeatList(event.reservation().getId());
        Member receiver = event.reservation().getMember();
        Instant now = Instant.now();

        // 2. 메시지 생성
        String title = formatTitle(template, performance);
        String message = formatMessage(template, performance, seatList, now);

        // 3. SSE 전송
        sendNotificationToClient(
                NotificationSseResponseDto.builder()
                        .title(title)
                        .message(message)
                        .build()
        );

        // 4. DB 저장
        saveNotification(receiver, template, now);
    }

    // 알림 약식을 가져오는 메서드
    private NotificationTemplate getTemplate(NotificationTemplateId templateId) {
        return notificationTemplateService.getNotificationTemplateById(templateId.getId());
    }

    // 예약번호로 자리리스트 반환 메서드
    private List<Seat> getSeatList(Long reservationId) {
        // TODO: seatService 주입받아 실제 구현
//        return seatService.getSeatListByReservationId(reservationId);
        return null;
    }

    // 제목을 형식에 맞게 수정해주는 메서드
    private String formatTitle(NotificationTemplate template, Performance performance) {
        return String.format(template.getTitle(), performance.getTitle());
    }

    // 메시지 형식에 맞게 수정해주는 메서드
    private String formatMessage(NotificationTemplate template, Performance performance, List<Seat> seats, Instant now) {
        String seatCodeString = String.join("\n", seats.stream().map(Seat::getSeatCode).toList());
        return String.format(
                template.getContent(),
                performance.getDate(),
                performance.getHall().getAddress(),
                seatCodeString,
                now
        );
    }

    // RDB에 알림 저장 메서드
    private void saveNotification(Member receiver, NotificationTemplate template, Instant createdAt) {
        Status unreadStatus = statusService.getReadYetStatusForNotification();
        Notification notification = Notification.builder()
                .receivedMember(receiver)
                .template(template)
                .status(unreadStatus)
                .createdAt(createdAt)
                .build();
        notificationRepository.save(notification);
    }

    // SSE 전송 메서드
    public void sendNotificationToClient(Object data) {
        String emitterId = SecurityUtil.getSignInMemberEmail();
        if (emitterMap.containsKey(emitterId)) { // 연결 존재 확인
            SseEmitter emitter = emitterMap.get(emitterId);
            try {
                emitter.send(SseEmitter.event()
                        .name(emitterId)
                        .data(data, MediaType.APPLICATION_JSON)
                        .id(String.valueOf(System.currentTimeMillis()))
                );
            } catch (IOException e) {
                // 전송 실패 시 emitter 제거
                emitterMap.remove(emitterId);
                emitter.completeWithError(e);
            }
        }
    }
}
