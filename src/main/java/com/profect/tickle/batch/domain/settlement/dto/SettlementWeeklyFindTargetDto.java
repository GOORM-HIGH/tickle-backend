package com.profect.tickle.batch.domain.settlement.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class SettlementWeeklyFindTargetDto {

    private Long memberId;
    private String performanceTitle;
    private String year;
    private String month;
    private String week;
    private Long weeklySalesAmount;
    private Long weeklyRefundAmount;
    private Long weeklyGrossAmount;
    private Long weeklyCommission;
    private Long weeklyNetAmount;
    private Instant settlementWeeklyCreatedAt;
}
