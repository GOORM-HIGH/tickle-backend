package com.profect.tickle.domain.settlement.dto.batch;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class SettlementDailyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private Instant performanceEndDate;
    private BigDecimal contractCharge;
    private String year;
    private String month;
    private String day;
    private Long dailySalesAmount;
    private Long dailyRefundAmount;
    private Long dailyGrossAmount;
    private Long dailyCommission;
    private Long dailyNetAmount;
    private Instant settlementDailyCreatedAt;
}
