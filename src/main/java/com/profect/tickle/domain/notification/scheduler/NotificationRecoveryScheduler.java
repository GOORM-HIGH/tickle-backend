package com.profect.tickle.domain.notification.scheduler;

import com.profect.tickle.domain.notification.service.realtime.MessageQueueRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRecoveryScheduler {

    private final MessageQueueRecoveryService redisMessageQueueRecoveryService;

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void recoverPendingMessages() {
        try {
            log.debug("메시지 복구 스케줄러 시작");

            redisMessageQueueRecoveryService.recoverPendingMessages();

            log.debug("메시지 복구 스케줄러 완료");

        } catch (Exception e) {
            log.error("Pending 메시지 복구 중 오류", e);
        }
    }
}
