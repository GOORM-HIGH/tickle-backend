package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementMonthlyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementMonthly;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface SettlementMonthlyMapper {

    /**
     * 연, 월, 주차로 주간 정산 조회
     */
    List<SettlementMonthlyFindTargetDto> findByWeek(HashMap<String, Object> map);

    /**
     * 월간 정산에 주간정산 데이터 upsert
     */
    void upsertSettlementMonthly(List<SettlementMonthly> list);

}
