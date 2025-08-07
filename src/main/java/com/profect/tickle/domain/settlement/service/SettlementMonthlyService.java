package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyDto;
import com.profect.tickle.domain.settlement.mapper.SettlementMonthlyMapper;
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
public class SettlementMonthlyService {

    private final SettlementMonthlyMapper settlementMonthlyMapper;

    public void getSettlementMonthly(){
        // 정산 시간
        LocalDateTime settlementDate = LocalDateTime.now();

        // 날짜 유틸
        LocalDate today = LocalDate.now();
        SettlementPeriod period = SettlementPeriod.get(today);
        int year = period.year();
        int month = period.month();
        int week = period.dayOfWeek();

        // 월간에 upsert할 주간 정산 조회
        HashMap<String, Object> map = new HashMap<>();
        map.put("year", String.valueOf(year));
        map.put("month", String.format("%02d", month));
        map.put("week", String.format("%02d", week));
        List<SettlementWeeklyDto> getList = settlementMonthlyMapper.findByWeek(map);

        // 월간에 upsert
        map.put("now", settlementDate);
        for(SettlementWeeklyDto dto : getList){
            map.put("dto", dto);
            settlementMonthlyMapper.upsertSettlementMonthly(map);
        }
    }
}
