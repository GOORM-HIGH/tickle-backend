package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface SettlementMonthlyMapper {

    /**
     * 연, 월, 주차로 주간 정산 조회
     */
    List<SettlementWeeklyDto> findByWeek(HashMap<String, Object> map);

    /**
     * 월간 정산에 주간정산 데이터 upsert
     */
    void upsertSettlementMonthly(HashMap<String, Object> map);

}
