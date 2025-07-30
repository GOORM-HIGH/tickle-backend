package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDTO;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotificationList() {
        // TODO: Mybatis 또는 JPA로 유저아이디를 통해 최신 10건 조회 메서드 구현
        List<Notification> notificationList = null;

        // Entity → DTO 변환
        return notificationList.stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }
}
