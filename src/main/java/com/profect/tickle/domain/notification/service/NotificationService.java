package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StatusService statusService;

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

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
}
