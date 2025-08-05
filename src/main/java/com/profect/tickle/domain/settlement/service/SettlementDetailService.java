package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailDto;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementDetailService {

    private final SettlementDetailMapper settlementDetailMapper;
    private final StatusRepository statusRepository;

    /**
     * 건별정산 연산 및 insert_tasklet 구조
     */
    public void getSettlementDetail(){
        // 정산 생성일시
        LocalDateTime settlementCreatedAt = LocalDateTime.now();

        // 건별정산 집계에 필요한 데이터
        List<SettlementDetailFindTargetDto> settlementTargets = settlementDetailMapper.findTargetReservations();
        log.info("예매 금액 테스트 ::::: ", settlementTargets);

        for(SettlementDetailFindTargetDto targetDto : settlementTargets) {
            Long reservationPrice = targetDto.getReservationPrice(); // 예매금액
            BigDecimal contractCharge = targetDto.getContractCharge(); // 수수료율
            // 판매금액, 환불금액 초기화
            Long salesAmount = 0L;
            Long refundAmount = 0L;

            // 정산상태 코드
            Long settlementStatusId;

            // 예매 상태 코드(102 = 결제, 103 = 취소)
            int reservationStatusCode = targetDto.getReservationStatusCode();

            if(reservationStatusCode == 102) {
                salesAmount = reservationPrice;
                refundAmount = 0L;
                settlementStatusId = 14L;
            } else if(reservationStatusCode == 103) {
                salesAmount = 0L;
                refundAmount = reservationPrice;
                settlementStatusId = 16L;
            } else {
                continue;
            }

            // 정산대상금액 = 판매금액
            Long grossAmount = salesAmount;
            // 수수료 = 판매금액 * 정산대상금액
            BigDecimal commission = contractCharge.multiply(BigDecimal.valueOf(grossAmount)).setScale(0, RoundingMode.HALF_UP);
            // 대납금액 = 정산대상금액 - 수수료
            BigDecimal netAmount = BigDecimal.valueOf(grossAmount).subtract(commission);

            // insert
            // dto에서 주최자 상호명, 공연제목, 예매 종료일시, 예매코드 추출
            // dto에서 예매상태코드(102, 103)에 따라 정산상태(14, 16)으로 분기해서 insert
            SettlementDetailDto detailDto = SettlementDetailDto.builder()
                    .statusId(settlementStatusId)
                    .hostBizName(targetDto.getHostBizName())
                    .performanceTitle(targetDto.getPerformanceTitle())
                    .performanceEndDate(targetDto.getPerformanceEndDate())
                    .reservationCode(targetDto.getReservationCode())
                    .createdAt(settlementCreatedAt)
                    .salesAmount(salesAmount)
                    .refundAmount(refundAmount)
                    .grossAmount(grossAmount)
                    .commission(commission.longValueExact())
                    .netAmount(netAmount.longValueExact()).build();

            log.info("건별정산 데이터: ", detailDto);
            settlementDetailMapper.insertSettlementDetail(detailDto);
        }
    }
}
