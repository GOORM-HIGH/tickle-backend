package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementWeeklyMapper;
import com.profect.tickle.domain.settlement.util.SettlementPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        // 일일정산 생성 시간
        LocalDateTime settlementDate = LocalDateTime.now();

        // yyyy, m, week, d(monday, sunday)
        LocalDate today = LocalDate.now();
        SettlementPeriod period = SettlementPeriod.get(today);
        int year = period.year();
        int month = period.month();
        int week = period.weekOfMonth();
        int monday = period.startOfWeek();
        int sunday = period.endOfWeek();
        log.info("회차 및 시작일, 종료일 ::: " + year + "-" + month + "월-" + week + "회차(" + monday + "일, " + sunday + "일)");

        // 오늘 날짜 기준 연월일로 일간정산 리스트 추출
        List<SettlementDailyDto> getDailyList = settlementWeeklyMapper.findByDate(settlementDate);

        // 주최자, 공연, 회차별로 upsert
        HashMap<String, Object> map = new HashMap<>();
        map.put("year", String.valueOf(year));
        map.put("month", String.format("%02d", month));
        map.put("week", String.format("%02d", week));
        map.put("now", settlementDate.minusDays(1));
        log.info("집계조회일 ::: " + settlementDate.minusDays(1));
        for(SettlementDailyDto dto : getDailyList) {
            map.put("dto", dto);
            // 날짜 정보, 일간 dto로 주간 테이블에 upsert
            settlementWeeklyMapper.upsertSettlementWeekly(map);
        }
    }
}
