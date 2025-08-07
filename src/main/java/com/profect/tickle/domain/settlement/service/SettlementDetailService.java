package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailDto;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementDetailService {

    private final SettlementDetailMapper settlementDetailMapper;

    /**
     * 건별정산 연산 및 insert_tasklet 구조
     */
    public void getSettlementDetail(){
        // 정산 생성일시
        Instant settlementCreatedAt = Instant.now();

        // 건별정산 집계에 필요한 데이터
        List<SettlementDetailFindTargetDto> settlementTargets;
        try  {
            settlementTargets = settlementDetailMapper.findTargetReservations();
        } catch (DataAccessException dae) {
            log.error("정산 대상 조회 중 DB 오류 발생", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
        }

        if(settlementTargets.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        for(SettlementDetailFindTargetDto targetDto : settlementTargets) {
            Long reservationPrice = targetDto.getReservationPrice(); // 예매금액
            BigDecimal contractCharge = targetDto.getContractCharge(); // 수수료율
            // 판매금액, 환불금액 초기화
            Long salesAmount;
            Long refundAmount;

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
                throw new BusinessException(ErrorCode.SETTLEMENT_INVALID_RESERVATION_STATUS);
            }

            // 정산대상금액 = 판매금액
            Long grossAmount = salesAmount;
            BigDecimal commission;
            BigDecimal netAmount;
            try {
                // 수수료 = 판매금액 * 정산대상금액
                commission = contractCharge.multiply(BigDecimal.valueOf(grossAmount)).setScale(0, RoundingMode.HALF_UP);
                // 대납금액 = 정산대상금액 - 수수료
                netAmount = BigDecimal.valueOf(grossAmount).subtract(commission);
            } catch (NullPointerException | ArithmeticException e) {
                // → contractCharge 가 null 이었거나
                //   BigDecimal 연산 중 뭔가 비정상적인 상황이 생겼을 때
                log.error("수수료 계산 오류: {}, 대상 DTO={}", e.getMessage(), targetDto);
                throw new BusinessException(ErrorCode.SETTLEMENT_COMMISSION_CALCULATION_ERROR);
            }

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

            try {
                settlementDetailMapper.insertSettlementDetail(detailDto);
            } catch(DataAccessException dae) {
                log.error("SettlementDetail insert 오류, DTO={}", detailDto);
                throw new BusinessException("SettlementDetail 테이블 insert 실패: " + detailDto,
                        ErrorCode.SETTLEMENT_INSERT_FAILED);
            }
        }
    }
}
