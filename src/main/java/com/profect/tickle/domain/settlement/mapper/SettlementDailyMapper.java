package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyDto;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface SettlementDailyMapper {

    /**
     * 건별정산 테이블 내역 주최자, 공연별로 일간 합산 리스트
     */
    List<SettlementDailyDto> aggregateByDetail(LocalDateTime now);

    /**
     * 일간정산 테이블 insert + update
     */
    void upsertSettlementDaily(HashMap<String, Object> map);

}
