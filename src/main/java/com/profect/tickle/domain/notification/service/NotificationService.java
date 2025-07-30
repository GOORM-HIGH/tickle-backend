package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDTO;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    //    private final SqlSession sqlSession;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotificationList(Long memberId) {

        List<NotificationResponseDTO> notificationList = notificationMapper.getRecentNotificationListByMemberId(memberId);


        // Entity → DTO 변환
        return notificationList;
    }
}
