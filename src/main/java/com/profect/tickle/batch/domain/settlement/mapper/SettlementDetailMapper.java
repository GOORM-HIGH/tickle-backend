package com.profect.tickle.batch.domain.settlement.mapper;

import com.profect.tickle.batch.domain.settlement.dto.SettlementDetailFindTargetDto;
import com.profect.tickle.batch.domain.settlement.dto.SettlementDetailFindTargetTestDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.global.status.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface SettlementDetailMapper {


    int findTargetReservationsCount(@Param("now") Instant now);

    /**
     * 건별정산에 필요한 데이터 추출
     */
    List<SettlementDetailFindTargetDto> findTargetFromReservations(@Param("now") Instant now);
    // 튜닝 전 테스트용
    List<SettlementDetailFindTargetTestDto> findTargetFromReservationsTest(@Param("now") Instant now);

    /**
     * 건별정산에 결과 insert
     */
    void insertSettlementDetail(@Param("list") List<SettlementDetail> list);

    /**
     * 건별 정산 상태 업데이트
     */
    void updateSettlementDetailStatus(@Param("beforeStatus") Status beforeStatus,
                                      @Param("afterStatus") Status afterStatus,
                                      @Param("endOfDay") Instant endOfDay);
}