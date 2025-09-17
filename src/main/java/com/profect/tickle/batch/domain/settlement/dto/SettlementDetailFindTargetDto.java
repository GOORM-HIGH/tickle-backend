package com.profect.tickle.batch.domain.settlement.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 건별정산 연산 데이터 추출 dto
 */

@Getter
public class SettlementDetailFindTargetDto {

    private Long reservationId;
    private Long reservationStatusId;
    private String reservationCode; // 예매 코드
    private Long reservationPrice; // 예매 금액
    private Long memberId;
    private String performanceTitle; // 공연 제목
    private Instant performanceEndDate; // 공연 예매 종료일시
    private BigDecimal contractCharge; // 정산 적용 수수료율
    private Instant settlementDetailCreatedAt;

    @Builder
    private SettlementDetailFindTargetDto(Long reservationId, Long reservationStatusId, String reservationCode, Long reservationPrice,
                                          Long memberId, String performanceTitle, Instant performanceEndDate,
                                          BigDecimal contractCharge, Instant settlementDetailCreatedAt) {
        this.reservationId = reservationId;
        this.reservationStatusId = reservationStatusId;
        this.reservationCode = reservationCode;
        this.reservationPrice = reservationPrice;
        this.memberId = memberId;
        this.performanceTitle = performanceTitle;
        this.performanceEndDate = performanceEndDate;
        this.contractCharge = contractCharge;
        this.settlementDetailCreatedAt = settlementDetailCreatedAt;
    }

    public static SettlementDetailFindTargetDto of(Long reservationId, Long reservationStatusId,
                                                   String reservationCode, Long reservationPrice, Long memberId,
                                                   String performanceTitle, Instant performanceEndDate,
                                                   BigDecimal contractCharge, Instant settlementDetailCreatedAt) {
        return SettlementDetailFindTargetDto.builder()
                .reservationId(reservationId)
                .reservationStatusId(reservationStatusId)
                .reservationCode(reservationCode)
                .reservationPrice(reservationPrice)
                .memberId(memberId)
                .performanceTitle(performanceTitle)
                .performanceEndDate(performanceEndDate)
                .contractCharge(contractCharge)
                .settlementDetailCreatedAt(settlementDetailCreatedAt)
                .build();
    }


 }
