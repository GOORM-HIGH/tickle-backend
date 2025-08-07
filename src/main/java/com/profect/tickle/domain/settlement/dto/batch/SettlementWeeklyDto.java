package com.profect.tickle.domain.settlement.dto.batch;

import com.profect.tickle.global.status.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementWeeklyDto {

    private Status statusId;
    private String hostBizName;
    private String performanceTitle;
    private String year;
    private String month;
    private String week;
    private Long weeklySalesAmount;
    private Long weeklyRefundAmount;
    private Long weeklyGrossAmount;
    private Long weeklyCommission;
    private Long weeklyNetAmount;
    private Instant weeklyCreatedAt;
}
