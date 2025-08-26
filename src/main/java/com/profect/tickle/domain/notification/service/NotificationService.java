package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    // utils
    private final StatusProvider statusProvider;

    // services
    private final MemberService memberService;

    // mappers & repositories
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    // 알림 조회 메서드
    public List<NotificationResponseDto> getNotificationListByMemberId(Long memberId, int limit) {
        return notificationMapper.getNotificationListByMemberId(memberId, limit);
    }

    // 신규 알림 저장 메서드
    @Transactional
    public Long saveNotification(
            @NotNull String toEmail,
            @NotNull NotificationTemplate template,
            @NotNull String title,
            @NotNull String content,
            @NotNull Instant createdAt
    ) {
        // 1) 수신자 조회 (없으면 내부에서 예외 발생하도록 설계 권장)
        Member receivedMember = memberService.getMemberByEmail(toEmail);

        // 2) 엔티티 생성
        Notification notification = Notification.builder()
                .receivedMember(receivedMember)
                .template(template)
                .title(title)
                .content(content)
                .createdAt(createdAt)
                .build();

        // 3) 저장 후 ID 반환
        return notificationRepository.save(notification).getId();
    }

    // 알림 읽음 처리 메서드
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        // 알림 조회한다
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 회원의 알림인지 확인한다.
        if (!notification.isForMember(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 읽음 처리한다.
        notification.markAsRead(statusProvider.provide(StatusIds.Notification.READ));
        log.info("{}님의 {}번 알림 읽음 처리 완료", memberId, notificationId);
    }
}
