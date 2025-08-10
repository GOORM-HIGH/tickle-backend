package com.profect.tickle.domain.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementStatusScheduler {

    private final SettlementDetailService settlementDetailService;
    private final SettlementDailyService settlementDailyService;
    private final SettlementWeeklyService settlementWeeklyService;
    private final SettlementMonthlyService settlementMonthlyService;

    /**
     * 매일 00시00분01초에 건별, 일별, 주간, 월간 정산 상태 업데이트
     */
    @Scheduled(cron = "1 0 0 * * *")
    public void updateSettlementStatus() {
        settlementDetailService.updateDetail(); // 건별 업데이트
        settlementDailyService.updateDailyByToday(); // 일별 업데이트(예매 종료일시 기준)
        settlementDailyService.updateDailyByBoundary(); // 일별 업데이트(월요일 또는 1일 기준)
        settlementWeeklyService.updateWeekly(); // 주간 업데이트(월요일 또는 1일 기준)
        settlementMonthlyService.updateMonthly(); // 월간 업데이트(1일 기준)
    }
}
