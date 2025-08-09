package com.profect.tickle.domain.settlement.dto.batch;

import com.profect.tickle.global.status.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDailyDto {

    private Status statusId;
    private String hostBizName;
    private String performanceTitle;
    private String year;
    private String month;
    private String day;
    private Long dailySalesAmount;
    private Long dailyRefundAmount;
    private Long dailyGrossAmount;
    private Long dailyCommission;
    private Long dailyNetAmount;
    private Instant dailyCreatedAt;
}
