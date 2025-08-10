package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementWeeklyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private Long weeklySalesAmount;
    private Long weeklyRefundAmount;
    private Long weeklyGrossAmount;
    private Long weeklyCommission;
    private Long weeklyNetAmount;
}
