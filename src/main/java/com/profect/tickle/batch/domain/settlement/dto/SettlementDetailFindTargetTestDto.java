package com.profect.tickle.batch.domain.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class SettlementDetailFindTargetTestDto {

    private Long memberId;
    private Long reservationStatusId;
    private String performanceTitle; // 공연 제목
    private Instant performanceEndDate; // 공연 예매 종료일시
    private String reservationCode; // 예매 코드
    private Long reservationPrice; // 예매 금액
    private BigDecimal contractCharge; // 정산 적용 수수료율
    private Instant settlementDetailCreatedAt;

    @Builder
    private SettlementDetailFindTargetTestDto(Long memberId, Long reservationStatusId, String performanceTitle, Instant performanceEndDate,
                                              String reservationCode, Long reservationPrice,
                                              BigDecimal contractCharge, Instant settlementDetailCreatedAt) {
        this.memberId = memberId;
        this.reservationStatusId = reservationStatusId;
        this.performanceTitle = performanceTitle;
        this.performanceEndDate = performanceEndDate;
        this.reservationCode = reservationCode;
        this.reservationPrice = reservationPrice;
        this.contractCharge = contractCharge;
        this.settlementDetailCreatedAt = settlementDetailCreatedAt;
    }

    public static SettlementDetailFindTargetTestDto of(Long memberId, String performanceTitle, Instant performanceEndDate,
                                                   Long reservationStatusId, String reservationCode, Long reservationPrice,
                                                   BigDecimal contractCharge, Instant settlementDetailCreatedAt) {
        return SettlementDetailFindTargetTestDto.builder()
                .memberId(memberId)
                .reservationStatusId(reservationStatusId)
                .performanceTitle(performanceTitle)
                .performanceEndDate(performanceEndDate)
                .reservationCode(reservationCode)
                .reservationPrice(reservationPrice)
                .contractCharge(contractCharge)
                .settlementDetailCreatedAt(settlementDetailCreatedAt)
                .build();
    }
}
