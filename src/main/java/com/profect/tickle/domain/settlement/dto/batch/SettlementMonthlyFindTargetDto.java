package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementMonthlyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private Long monthlySalesAmount;
    private Long monthlyRefundAmount;
    private Long monthlyGrossAmount;
    private Long monthlyCommission;
    private Long monthlyNetAmount;
}
