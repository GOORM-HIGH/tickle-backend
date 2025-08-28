package com.profect.tickle.domain.performance.scheduler;

import com.profect.tickle.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceStatusScheduler {

    private final PerformanceRepository performanceRepository;

    /**
     * 매일 새벽 자정(Asia/Seoul)에 전체 상태 갱신
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateAllStatusesDaily() {
        int updated = performanceRepository.updateAllStatusesByDateRule();
        log.info("🎭 공연 상태 일괄 갱신 완료 - 총 {}건", updated);
    }
}
