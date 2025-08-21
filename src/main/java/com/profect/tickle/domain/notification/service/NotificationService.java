package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.mapper.NotificationTemplateMapper;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.service.mail.MailSender;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    // utils
    private final StatusProvider statusProvider;
    private final MailSender mailSender;
    private final NotificationProperty notificationProperty;

    // services
    private final MemberService memberService;
    private final NotificationTemplateService notificationTemplateService;
    private final PerformanceService performanceService;
    private final ReservationService reservationService;

    // mappers & repositories
    private final NotificationTemplateMapper notificationTemplateMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final SseRepository sseRepository;

    // 알림 조회 메서드
    public List<NotificationResponseDto> getNotificationListByMemberId(Long memberId, int limit) {
        return notificationMapper.getNotificationListByMemberId(memberId, limit);
    }

    // 알림 읽음 처리 메서드
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        // 알림 조회한다
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 회원의 알림인지 확인한다.
        if (!notification.getReceivedMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 읽음 처리한다.
        notification.markAsRead(statusProvider.provide(StatusIds.Notification.READ));
    }

    /**
     * 예매 성공 알림
     */
//    public void sendReservationSuccessNotification(ReservationSuccessEvent event) {
//        sendPerformanceNotification(
//                NotificationTemplateId.RESERVATION_SUCCESS,
//                event.performance(),
//                List.of(event.reservation()),
//                event.member().getEmail()
//        );
//    }

    /**
     * 공연 수정 알림
     */
//    public void sendPerformanceModifiedNotification(PerformanceModifiedEvent event) {
//        sendPerformanceNotification(
//                NotificationTemplateId.PERFORMANCE_MODIFIED,
//                event.performance(),
//                event.reservationList(),
//                event.member().getEmail()
//        );
//    }

    /**
     * 공통 Performance 알림 전송
     */
//    private void sendPerformanceNotification(
//            NotificationTemplateId templateId,
//            PerformanceDto performance,
//            List<ReservationDto> reservationList,
//            String email
//    ) {
//        NotificationTemplate template = getTemplate(templateId);
//        String title = String.format(template.getTitle(), performance.getTitle());
//
//        if (templateId == NotificationTemplateId.RESERVATION_SUCCESS) {
//            // 단일 예약 성공 알림
//            ReservationDto reservation = reservationList.getFirst(); // 또는 reservationList.get(0)
//            List<ReservedSeatDto> seatList = reservationService.getSeatListByReservationId(reservation.getId());
//            String seatCodes = seatList.stream()
//                    .map(ReservedSeatDto::getSeatCode)
//                    .collect(Collectors.joining("\n"));
//
//            String message = String.format(
//                    template.getContent(),
//                    performance.getTitle(),
//                    performance.getDate(),
//                    seatCodes
//            );
//
//            sendSseAndSaveNotification(email, template, title, message, Instant.now());
//
//        } else if (templateId == NotificationTemplateId.PERFORMANCE_MODIFIED) {
//            // 공연 정보 수정 시 모든 예매 건에 대해 각각 알림 전송
//            if (reservationList.isEmpty()) {
//                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
//            }
//
//            for (ReservationDto reservation : reservationList) {
//                String newTitle = "[공연제목]: " + performance.getTitle();
//                String newDate = "[일  자]: " + performance.getDate();
//                String newImg = "[이미지]: " + performance.getImg();
//
//                String newContent = String.join("\n", newTitle, newDate, newImg);
//
//                String message = String.format(
//                        template.getContent(),
//                        newContent
//                );
//
//                sendSseAndSaveNotification(email, template, title, message, Instant.now());
//            }
//
//        } else {
//            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
//        }
//    }

//    // SSE 전송 + Mail 전송 메서드
//    private void sendSseAndSaveNotification(
//            String memberEmail, // 받는 유저 이메일
//            NotificationTemplate template, // 템플릿 유형
//            String subject, // 제목
//            String content, // 메시지
//            Instant createdAt // 생성일
//    ) {
//        // SSE 전송
//        NotificationSseResponseDto dto = NotificationSseResponseDto.builder()
//                .title(subject)
//                .message(content)
//                .build();
//
//        String json = convertToJson(dto);
//        sendSseNotification(memberEmail, json);
//
//        // 메일 발송
//        mailSender.sendText(new MailCreateServiceRequestDto(memberEmail, subject, content));
//
//        // DB 저장
//        Member member = memberService.getMemberByEmail(memberEmail);
//        notificationRepository.save(Notification.builder()
//                .receivedMember(member)
//                .template(template)
//                .title(subject)
//                .content(content)
//                .status(statusProvider.provide(StatusIds.Notification.UNREAD))
//                .createdAt(createdAt)
//                .build());
//    }
}
