package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementWeekly;
import com.profect.tickle.global.status.Status;
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

    /**
     * 월요일 또는 1일 기준 n-1회차 정산 상태 업데이트
     */
    void updateSettlementDetailStatus(@Param("beforeStatus") Status beforeStatus,
                                      @Param("afterStatus") Status afterStatus,
                                      @Param("year") String year,
                                      @Param("month") String month,
                                      @Param("week") String week);
}
