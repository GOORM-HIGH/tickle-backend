package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementDailyMapper;
import com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementDailyService {

    private final SettlementDetailMapper settlementDetailMapper;
    private final SettlementDailyMapper settlementDailyMapper;

    /**
     * 일간정산 테이블 insert+update_tasklet 구조
     */
    public void getSettlementDaily() {
        Instant now = Instant.now();

        // 건별정산 데이터 집계 조회
        List<SettlementDailyDto> aggregates;
        try {
            aggregates = settlementDailyMapper.aggregateByDetail();
        } catch (DataAccessException dae) {
            log.error("정산 대상 조회 중 DB 오류 발생", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
        }

        if(aggregates.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        // 일간정산 테이블 upsert
        HashMap<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("now", now);
        for(SettlementDailyDto dto : aggregates) {
            paramsMap.put("dto", dto);
            try{
                settlementDailyMapper.upsertSettlementDaily(paramsMap);
            } catch (DataAccessException dae){
                log.error("SettlementDaily upsert 오류, DTO={}", dto);
                throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
            }
        }

        // 건별정산 테이블 상태 업데이트(정산 집계 완료 상태 N -> Y)
//        try {
//            settlementDetailMapper.updateSettlementDetail(now);
//        } catch (DataAccessException dae){
//            log.error("정산 상태 업데이트에 실패했습니다.");
//            throw new BusinessException(ErrorCode.SETTLEMENT_STATUS_UPDATE_FAILED);
//        }
    }
}
