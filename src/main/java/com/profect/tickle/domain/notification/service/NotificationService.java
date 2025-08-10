package com.profect.tickle.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.profect.tickle.domain.performance.dto.response.PerformanceDto;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservationDto;
import com.profect.tickle.domain.reservation.dto.response.reservation.ReservedSeatDto;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.security.util.SecurityUtil;
import com.profect.tickle.global.status.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final Long TIME_OUT = 60 * 60 * 1000L;

    private final MemberService memberService;
    private final StatusService statusService;
    private final NotificationTemplateService notificationTemplateService;
    private final PerformanceService performanceService;
    private final MailService mailService;
    private final ReservationService reservationService;

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
        log.info("📡 SSE 연결 요청 - emitterId: {}", emitterId);

        if (emitterId == null || emitterId.isBlank()) {
            log.warn("❌ emitterId가 null이거나 공백입니다. 인증된 사용자 정보가 없습니다.");
        }

        SseEmitter emitter = new SseEmitter(TIME_OUT);
        sseRepository.save(emitterId, emitter);
        log.info("✅ SSE emitter 저장 완료 - ID: {}", emitterId);

        emitter.onCompletion(() -> {
            log.info("🧹 SSE 연결 종료 (onCompletion) - ID: {}", emitterId);
            sseRepository.deleteById(emitterId);
        });

        emitter.onTimeout(() -> {
            log.warn("⏱️ SSE 타임아웃 발생 - ID: {}", emitterId);
            sseRepository.deleteById(emitterId);
        });

        try {
            emitter.send(SseEmitter.event().name("sse connect").data("connected"));
            log.info("✅ SSE 초기 메시지 전송 완료 - ID: {}", emitterId);
        } catch (IOException e) {
            log.error("❌ SSE 초기 메시지 전송 실패 - ID: {}, 오류: {}", emitterId, e.getMessage());
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
        if (emitter == null) {
            log.warn("❗ SSE Emitter not found for ID: {}", id);
            return;
        }

        String eventId = String.valueOf(System.currentTimeMillis());

        try {
            log.info("📤 SSE 알림 전송 시작 - ID: {}, EventID: {}, Message: {}", id, eventId, message);

            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(message, MediaType.APPLICATION_JSON)
                    .id(eventId));

            sseRepository.saveEvent(eventId, message);

            log.info("✅ SSE 알림 전송 완료 - ID: {}, EventID: {}", id, eventId);

        } catch (IOException e) {
            log.error("❌ SSE 전송 실패 - ID: {}, 오류: {}", id, e.getMessage());
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
    public void sendCouponAlmostExpiredNotification(
            String memberEmail, // 알림는 유저 이메일
            String couponName, // 쿠폰 이름
            Instant expiryDate // 만료 일자
    ) {
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
        sendPerformanceNotification(
                NotificationTemplateId.RESERVATION_SUCCESS,
                event.performance(),
                List.of(event.reservation()),
                event.member().getEmail()
        );
    }

    /**
     * 공연 수정 알림
     */
    public void sendPerformanceModifiedNotification(PerformanceModifiedEvent event) {
        sendPerformanceNotification(
                NotificationTemplateId.PERFORMANCE_MODIFIED,
                event.performance(),
                event.reservationList(),
                event.member().getEmail()
        );
    }

    /**
     * 공통 Performance 알림 전송
     */
    private void sendPerformanceNotification(
            NotificationTemplateId templateId,
            PerformanceDto performance,
            List<ReservationDto> reservationList,
            String email
    ) {
        NotificationTemplate template = getTemplate(templateId);
        String title = String.format(template.getTitle(), performance.getTitle());

        if (templateId == NotificationTemplateId.RESERVATION_SUCCESS) {
            // 단일 예약 성공 알림
            ReservationDto reservation = reservationList.getFirst(); // 또는 reservationList.get(0)
            List<ReservedSeatDto> seatList = reservationService.getSeatListByReservationId(reservation.getId());
            String seatCodes = seatList.stream()
                    .map(ReservedSeatDto::getSeatCode)
                    .collect(Collectors.joining("\n"));

            String message = String.format(
                    template.getContent(),
                    performance.getTitle(),
                    performance.getDate(),
                    seatCodes
            );

            sendSseAndSaveNotification(email, template, title, message, Instant.now());

        } else if (templateId == NotificationTemplateId.PERFORMANCE_MODIFIED) {
            // 공연 정보 수정 시 모든 예매 건에 대해 각각 알림 전송
            if (reservationList.isEmpty()) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }

            for (ReservationDto reservation : reservationList) {
                String newTitle = "[공연제목]: " + performance.getTitle();
                String newDate = "[일  자]: " + performance.getDate();
                String newImg = "[이미지]: " + performance.getImg();

                String newContent = String.join("\n", newTitle, newDate, newImg);

                String message = String.format(
                        template.getContent(),
                        newContent
                );

                sendSseAndSaveNotification(email, template, title, message, Instant.now());
            }

        } else {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
        }
    }

    /**
     * 알림 전송 + 저장 + 메일 발송
     */
    private void sendSseAndSaveNotification(
            String memberEmail, // 받는 유저 이메일
            NotificationTemplate template, // 템플릿 유형
            String title, // 제목
            String message, // 메시지
            Instant createdAt // 생성일
    ) {
        // SSE 전송
        NotificationSseResponseDto dto = NotificationSseResponseDto.builder()
                .title(title)
                .message(message)
                .build();

        String json = convertToJson(dto);
        sendSseNotification(memberEmail, json);

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

    // Json으로 변환하는 메서드
    private String convertToJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON 직렬화 실패", e);
            return "{}";
        }
    }

}
