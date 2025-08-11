package com.profect.tickle.domain.settlement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponseDto {

    private Long memberId;
    private String performanceTitle;
    private Long salesAmount;
    private Long refundAmount;
    private Long grossAmount;
    private int contractCharge;
    private Long commission;
    private Long netAmount;
    private String statusName;
    private LocalDateTime settlementDate;
    private String settlementCycle;
}
