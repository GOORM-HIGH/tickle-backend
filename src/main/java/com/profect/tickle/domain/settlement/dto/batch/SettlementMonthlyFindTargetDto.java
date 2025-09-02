package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementMonthlyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private String year;
    private String month;
    private Long monthlySalesAmount;
    private Long monthlyRefundAmount;
    private Long monthlyGrossAmount;
    private Long monthlyCommission;
    private Long monthlyNetAmount;
    private Instant settlementMonthlyCreatedAt;
}
