package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementDailyMapper;
import com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        LocalDateTime now = LocalDateTime.now();

        // 건별정산 데이터 집계 조회
        List<SettlementDailyDto> aggregates = settlementDailyMapper.aggregateByDetail(now);

        // 일간정산 테이블 upsert
        HashMap<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("now", now);
        for(SettlementDailyDto dto : aggregates) {
            paramsMap.put("dto", dto);
            settlementDailyMapper.upsertSettlementDaily(paramsMap);
        }

        // 건별정산 테이블 상태 업데이트(정산 집계 완료 상태 N -> Y)
        settlementDetailMapper.updateSettlementDetail(now);
    }
}
