package com.profect.tickle.domain.settlement.dto.batch;

import com.profect.tickle.global.status.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementMonthlyDto {

    private Status statusId;
    private String hostBizName;
    private String performanceTitle;
    private String year;
    private String month;
    private String week;
    private Long monthlySalesAmount;
    private Long monthlyRefundAmount;
    private Long monthlyGrossAmount;
    private Long monthlyCommission;
    private Long monthlyNetAmount;
    private Instant monthlyCreatedAt;
}
