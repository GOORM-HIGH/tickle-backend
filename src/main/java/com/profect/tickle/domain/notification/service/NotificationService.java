package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
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

    // mappers & repositories
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

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
}
