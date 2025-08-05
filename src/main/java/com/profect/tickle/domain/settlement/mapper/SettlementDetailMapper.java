package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailDto;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SettlementDetailMapper {

    /**
     * 건별정산에 필요한 데이터 추출
     */
    List<SettlementDetailFindTargetDto> findTargetReservations();

    /**
     * 건별정산에 결과 insert
     * @param dto
     */
    void insertSettlementDetail(SettlementDetailDto dto);

    /**
     * 건별정산 상태 update
     */
    void updateSettlementDetail(LocalDateTime now);
}