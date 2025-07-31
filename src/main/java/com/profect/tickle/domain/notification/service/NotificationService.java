package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationList(Long memberId) {

        List<NotificationResponseDto> notificationList = notificationMapper.getRecentNotificationListByMemberId(memberId);

        return notificationList;
    }
}
