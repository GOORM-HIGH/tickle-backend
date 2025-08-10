package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementDetailMapper {

    /**
     * 건별정산에 필요한 데이터 추출
     */
    List<SettlementDetailFindTargetDto> findTargetReservations();

    /**
     * 건별정산에 결과 insert
     */
    void insertSettlementDetail(@Param("list") List<SettlementDetail> list);
}