package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementDailyMapper {

    /**
     * 건별정산 테이블 내역 주최자, 공연별로 일간 합산 리스트
     */
    List<SettlementDailyFindTargetDto> aggregateByDetail();

    /**
     * 일간정산 테이블 insert + update
     */
    void upsertSettlementDaily(@Param("list") List<SettlementDaily> list);

}
