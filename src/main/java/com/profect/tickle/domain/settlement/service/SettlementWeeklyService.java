package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementWeeklyMapper;
import com.profect.tickle.domain.settlement.util.SettlementPeriod;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementWeeklyService {

    private final SettlementWeeklyMapper settlementWeeklyMapper;

    /**
     * 주간정산 테이블 insert+update_tasklet 구조
     */
    public void getSettlementWeekly(){
        HashMap<String, Object> map = new HashMap<>();

        // 정산 생성 시간
        Instant settlementDate = Instant.now();

        // 날짜 유틸 yyyy, m, week
        // 00시00분30초에 전날의 정산내역 집계 (yesterday)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SettlementPeriod period = SettlementPeriod.get(yesterday);
        map.put("year", period.yearStr());
        map.put("month", period.monthStr());
        map.put("day", period.dayOfMonthStr());
        map.put("week", period.weekOfMonthStr());
        map.put("now", settlementDate);

        // 오늘 날짜 기준 연월일로 일간정산 리스트 추출
        List<SettlementDailyDto> getDailyList;
        try {
            getDailyList = settlementWeeklyMapper.findByDate(map);
        } catch (DataAccessException dae) {
            log.error("정산 대상 조회 중 DB 오류 발생", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
        }

        if(getDailyList.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        // 주최자, 공연, 회차별로 upsert
        for(SettlementDailyDto dto : getDailyList) {
            map.put("dto", dto);
            // 날짜 정보, 일간 dto로 주간 테이블에 upsert
            try {
                settlementWeeklyMapper.upsertSettlementWeekly(map);
            } catch (DataAccessException dae) {
                log.error("SettlementWeekly upsert 오류, DTO={}", dto);
                throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
            }
        }
    }
}
