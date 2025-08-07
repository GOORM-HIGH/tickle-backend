package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementMonthlyMapper;
import com.profect.tickle.domain.settlement.util.SettlementPeriod;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementMonthlyService {

    private final SettlementMonthlyMapper settlementMonthlyMapper;

    public void getSettlementMonthly(){
        HashMap<String, Object> map = new HashMap<>();

        // 정산 생성 시간
        LocalDateTime settlementDate = LocalDateTime.now();

        // 날짜 유틸 yyyy, m, week
        LocalDate today = LocalDate.now();
        SettlementPeriod period = SettlementPeriod.get(today);
        map.put("year", period.yearStr());
        map.put("month", period.monthStr());
        map.put("week", period.weekOfMonthStr());

        // 월간에 upsert할 주간 정산 조회
        List<SettlementWeeklyDto> getWeeklyList;
        try {
            getWeeklyList = settlementMonthlyMapper.findByWeek(map);
        } catch (DataAccessException dae) {
            log.error("정산 대상 조회 중 DB 오류 발생", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
        }

        if(getWeeklyList.isEmpty()){
            log.error("정산 대상 데이터가 존재하지 않습니다.");
        }

        // 월간에 upsert
        map.put("now", settlementDate);
        for(SettlementWeeklyDto dto : getWeeklyList){
            map.put("dto", dto);
            try {
                settlementMonthlyMapper.upsertSettlementMonthly(map);
            } catch (DataAccessException dae) {
                log.error("SettlementMonthly upsert 오류, DTO={}", dto);
                throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
            }
        }
    }
}
