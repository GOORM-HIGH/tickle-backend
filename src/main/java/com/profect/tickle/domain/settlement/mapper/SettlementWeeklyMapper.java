package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementWeekly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface SettlementWeeklyMapper {

    /**
     * 입력받은 날의 일간정산 데이터 추출
     */
    List<SettlementWeeklyFindTargetDto> findByDate(HashMap<String, Object> map);

    /**
     * 추출한 일간정산 데이터 연, 월, 주차, 상호명, 공연 유니크로 upsert
     */
    void upsertSettlementWeekly(@Param("list") List<SettlementWeekly> list);
}
