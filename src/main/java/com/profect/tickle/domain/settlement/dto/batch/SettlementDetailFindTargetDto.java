package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 건별정산 연산 데이터 추출 dto
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetailFindTargetDto {

    private String hostBizName; // 주최자 상호명
    private String performanceTitle; // 공연 제목
    private Instant performanceEndDate; // 공연 예매 종료일시
    private String reservationCode; // 예매 코드
    private Long reservationPrice; // 예매 가격
    private Integer reservationStatusCode; // 예매 상태
    private BigDecimal contractCharge; // 주최자 수수료율
}
