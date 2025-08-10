package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDailyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private Instant performanceEndDate;
    private String year;
    private String month;
    private String day;
    private Long dailySalesAmount;
    private Long dailyRefundAmount;
    private Long dailyGrossAmount;
    private BigDecimal contractCharge;
    private Long dailyCommission;
    private Long dailyNetAmount;
}
