package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.response.SettlementResponseDto;
import com.profect.tickle.global.status.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SettlementResponseMapper {

    /**
     * 주최자별, 공연별 건별, 일별, 주간, 월간 정산 내역
     */
    List<SettlementResponseDto> searchDefault(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param(("endDate")) LocalDate endDate,
            @Param("performanceTitle") String performanceTitle,
            @Param("status") Status status,
            @Param("limit") int size,
            @Param("offset") int offset);

    /**
     * 주최자별 일별, 주간, 월간 합산 정산 내역
     */
    List<SettlementResponseDto> searchByHost(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param(("endDate")) LocalDate endDate,
            @Param("status") Status status,
            @Param("limit") int size,
            @Param("offset") int offset);

    /**
     * 주최자별, 공연별 건별, 일별, 주간, 월간 정산 내역 총 건수
     */
    int searchDefaultCnt(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param(("endDate")) LocalDate endDate,
            @Param("performanceTitle") String performanceTitle,
            @Param("status") Status status);

    /**
     * 주최자별 일별, 주간, 월간 합산 정산 내역 총 건수
     */
    int searchByHostCnt(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param(("endDate")) LocalDate endDate,
            @Param("status") Status status);

    /**
     * 일별 테이블에서 미정산 금액 조회
     */
    Long sumUnsettledAmount(Long memberId, Status status);

    /**
     * 엑셀 다운로드용 청크 단위 조회
     */
    List<SettlementResponseDto> searchForExcel(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("viewType") String viewType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("performanceTitle") String performanceTitle,
            @Param("status") Status status,
            @Param("chunkSize") int chunkSize,
            @Param("offset") int offset
    );

    /**
     * Host View용 엑셀 다운로드 청크 단위 조회
     */
    List<SettlementResponseDto> searchByHostForExcel(
            @Param("memberId") Long memberId,
            @Param("periodType") String periodType,
            @Param("settlementCycle") String settlementCycle,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") Status status,
            @Param("chunkSize") int chunkSize,
            @Param("offset") int offset
    );
}
