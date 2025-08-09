package com.profect.tickle.domain.settlement.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 건별정산 조회용 dto
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDetailDto {

    private Long statusId;
    private String hostBizName; // 주최자 상호명
    private String performanceTitle; // 공연 제목
    private Instant performanceEndDate; // 공연 예매 종료일시
    private String reservationCode; // 예매 코드
    private Instant createdAt; // 정산 생성일시
    private Long salesAmount; // 판매금액
    private Long refundAmount; // 환불금액
    private Long grossAmount; // 정산대상금액(판매금액)
    private Long commission; // 수수료(정산대상금액 * 수수료율)
    private Long netAmount; // 대납금액(정산대산금액-수수료)
}
