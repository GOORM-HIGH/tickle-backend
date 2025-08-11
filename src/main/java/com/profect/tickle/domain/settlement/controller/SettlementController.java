package com.profect.tickle.domain.settlement.controller;

import com.profect.tickle.domain.settlement.dto.response.SettlementResponseDto;
import com.profect.tickle.domain.settlement.service.SettlementResponseService;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@Tag(name = "정산", description = "정산 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class SettlementController {

    private final SettlementResponseService settlementResponseService;

    @Operation(summary = "조건별 정산 내역 조회", description = "조건별 정산 내역을 페이지당 10건씩 조회합니다.")
    @GetMapping("/settlements")
    public ResultResponse<PagingResponse<SettlementResponseDto>> getSettlement(
            @RequestParam(value = "periodType", defaultValue = "DETAIL") SettlementResponseService.PeriodType periodType,
            @RequestParam(value = "viewType", defaultValue = "PERFORMANCE") SettlementResponseService.ViewType viewType,
            @RequestParam(value = "settlementCycle", required = false) String settlementCycle,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "performanceTitle", required = false) String performanceTitle,
            @RequestParam(value = "statusId", required = false) Long statusId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {

        Long signInMemberId = SecurityUtil.getSignInMemberId();
        PagingResponse<SettlementResponseDto> response = settlementResponseService.getSettlementList(
                signInMemberId, periodType, viewType, settlementCycle,
                startDate, endDate, performanceTitle, statusId, page, size);

        return ResultResponse.of(ResultCode.SETTLEMENT_LIST_SUCCESS, response);
    }

    @Operation(summary = "미정산 내역 조회", description = "미정산 내역 합산을 조회합니다.")
    @GetMapping("/settlements/unsettled-amount")
    public ResultResponse<Long> getUnsettledAmount() {
        Long signInMemberId = SecurityUtil.getSignInMemberId();
        Long unsettledAmount = settlementResponseService.getUnsettledAmount(signInMemberId);
        System.out.println("unsettledAmount: " + unsettledAmount);
        return ResultResponse.of(ResultCode.SETTLEMENT_UNSETTLED_AMOUNT_SUCCESS, unsettledAmount);
    }

    /**
     * 엑셀 다운로드
     */
    @GetMapping("/settlements/excel")
    public void downloadExcel(
            @RequestParam(value = "periodType", defaultValue = "DETAIL") SettlementResponseService.PeriodType periodType,
            @RequestParam(value = "viewType", defaultValue = "PERFORMANCE") SettlementResponseService.ViewType viewType,
            @RequestParam(value = "settlementCycle", required = false) String settlementCycle,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "performanceTitle", required = false) String performanceTitle,
            @RequestParam(value = "statusId", required = false) Long statusId,
            HttpServletResponse response
    ) {
        try {
            Long signInMemberId = SecurityUtil.getSignInMemberId();

            log.info("엑셀 다운로드 시작: memberId={}, periodType={}, viewType={}",
                    signInMemberId, periodType, viewType);

            settlementResponseService.generateExcelFile(
                    signInMemberId, periodType, viewType, settlementCycle,
                    startDate, endDate, performanceTitle, statusId, response
            );

            log.info("엑셀 다운로드 완료: memberId={}", signInMemberId);

        } catch (Exception e) {
            log.error("엑셀 다운로드 실패: {}", e.getMessage(), e);

            // 에러 응답 처리
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write("엑셀 다운로드에 실패했습니다: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("에러 응답 작성 실패", ioException);
            }
        }
    }
}
