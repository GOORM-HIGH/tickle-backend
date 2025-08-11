package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementMonthlyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementMonthly;
import com.profect.tickle.global.status.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 1일 기준 지난 달 정산 상태 업데이트
     */
    void updateSettlementMonthlyStatus(@Param("beforeStatus") Status beforeStatus,
                                       @Param("afterStatus") Status afterStatus,
                                       @Param("year") String year,
                                       @Param("month") String month);
}
