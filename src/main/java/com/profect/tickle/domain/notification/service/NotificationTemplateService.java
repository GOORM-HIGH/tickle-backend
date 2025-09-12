package com.profect.tickle.domain.notification.service;

import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.mapper.NotificationTemplateMapper;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateMapper notificationTemplateMapper;

    public NotificationTemplate getNotificationTemplateById(Long templateId) {
        log.info("알림 템플릿 조회 시작");
        long startTime = System.currentTimeMillis();

        NotificationTemplate template = notificationTemplateMapper.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));

        long endTime = System.currentTimeMillis();
        log.info("알림 템플리 조회 종료. 소요시간: {}", (endTime - startTime));
        return template;
    }
}

