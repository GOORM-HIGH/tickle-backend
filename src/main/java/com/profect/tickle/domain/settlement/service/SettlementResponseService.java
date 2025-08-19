package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.settlement.dto.response.SettlementResponseDto;
import com.profect.tickle.domain.settlement.mapper.SettlementResponseMapper;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.paging.PagingResponse;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.StatusIds.Settlement;
import com.profect.tickle.global.status.repository.StatusRepository;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementResponseService {
    public enum PeriodType {
        DETAIL,
        DAILY,
        WEEKLY,
        MONTHLY
    }
    public enum ViewType {
        PERFORMANCE,
        HOST
    }

    private final SettlementResponseMapper settlementResponseMapper;
    private final StatusRepository statusRepository;
    private final StatusProvider statusProvider;
    private static final int CHUNK_SIZE = 10000; // 1만 건씩 청크 처리
    private static final int WINDOW_SIZE = 100;

    /**
     * 정산 내역 조회(기간별, 옵션별 분기)
     */
    public PagingResponse<SettlementResponseDto> getSettlementList(
            Long memberId, PeriodType periodType, ViewType viewType,
            String settlementCycle, LocalDate startDate, LocalDate endDate,
            String performanceTitle, Long statusId, int page, int size) {

        // 건별 정산(주최자 합산 없으므로 공연으로 강제반환)
        if(periodType == PeriodType.DETAIL && viewType == ViewType.HOST) {
            viewType = ViewType.PERFORMANCE;
        }

        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        List<SettlementResponseDto> resultList; // 결과 리스트
        int offset = (page - 1) * size;
        int totalCounts;

        if(viewType == ViewType.PERFORMANCE) {
            // 주최자별, 공연별 건별, 일별, 주간, 월간 정산 내역
            resultList = settlementResponseMapper.searchDefault(
                    memberId, periodType.toString(), settlementCycle, startDate, endDate,
                    performanceTitle, status, size, offset);
            // 총 건수
            totalCounts = settlementResponseMapper.searchDefaultCnt(
                    memberId, periodType.toString(), settlementCycle, startDate, endDate,
                    performanceTitle, status);
        } else {
            //            // 주최자별 일별, 주간, 월간 정산 내역
            resultList = settlementResponseMapper.searchByHost(
                    memberId, periodType.toString(), settlementCycle,
                    startDate, endDate, status, size, offset);
            // 총 건수
            totalCounts = settlementResponseMapper.searchByHostCnt(
                    memberId, periodType.toString(), settlementCycle,
                    startDate, endDate, status);
        }

        return PagingResponse.from(resultList, page, size, totalCounts);
    }

    /**
     * 미정산 내역 조회
     */
    public Long getUnsettledAmount(Long memberId) {
        // 일별정산 테이블에서 '정산예정' 상태인 건들의 대납금액 합계
        Status status = statusProvider.provide(Settlement.SCHEDULED);
        return settlementResponseMapper.sumUnsettledAmount(memberId, status);
    }

    /**
     * 엑셀 다운로드용 데이터 조회 (청크 단위)
     */
    public void generateExcelFile(
            Long memberId,
            PeriodType periodType,
            ViewType viewType,
            String settlementCycle,
            LocalDate startDate,
            LocalDate endDate,
            String performanceTitle,
            Long statusId,
            HttpServletResponse response
    ) throws IOException {

        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        // 총 건수 확인
        int totalCount = getTotalCount(memberId, periodType, viewType, settlementCycle,
                startDate, endDate, performanceTitle, status);

        if (totalCount == 0) {
            // 0건일 때 예외 던지지 않고 응답 처리
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("다운로드할 정산 내역이 없습니다.");
            return;
        }

        // SXSSFWorkbook 사용 (메모리 효율적)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW_SIZE)) {
            // 압축 설정으로 파일 크기 최적화
            workbook.setCompressTempFiles(true);

            Sheet sheet = workbook.createSheet("정산내역");

            // 헤더 생성
            createHeader(sheet);

            // 데이터 청크 단위로 처리
            int rowIndex = 1;
            int offset = 0;

            while (offset < totalCount) {
                List<SettlementResponseDto> chunk = getDataChunk(
                        memberId, periodType, viewType, settlementCycle,
                        startDate, endDate, performanceTitle, status, offset
                );

                // 청크 데이터를 엑셀에 추가
                for (SettlementResponseDto item : chunk) {
                    createDataRow(sheet, rowIndex++, item, periodType, viewType);
                }

                offset += CHUNK_SIZE;

                // 진행률 로깅
                log.info("엑셀 생성 진행률: {}/{} ({}%)",
                        Math.min(offset, totalCount), totalCount,
                        (Math.min(offset, totalCount) * 100 / totalCount));
            }

            // 파일 다운로드
            downloadExcelFile(workbook, response, "정산내역");
        }
    }

    /**
     * 청크 단위 데이터 조회
     */
    private List<SettlementResponseDto> getDataChunk(
            Long memberId, PeriodType periodType, ViewType viewType,
            String settlementCycle, LocalDate startDate, LocalDate endDate,
            String performanceTitle, Status status, int offset
    ) {
        if (viewType == ViewType.HOST) {
            return settlementResponseMapper.searchByHostForExcel(
                    memberId, periodType.toString(), settlementCycle,
                    startDate, endDate, status, CHUNK_SIZE, offset
            );
        } else {
            return settlementResponseMapper.searchForExcel(
                    memberId, periodType.toString(), viewType.toString(),
                    settlementCycle, startDate, endDate, performanceTitle,
                    status, CHUNK_SIZE, offset
            );
        }
    }

    /**
     * 총 건수 조회
     */
    private int getTotalCount(Long memberId, PeriodType periodType, ViewType viewType,
                              String settlementCycle, LocalDate startDate, LocalDate endDate,
                              String performanceTitle, Status status) {
        if (viewType == ViewType.HOST) {
            return settlementResponseMapper.searchByHostCnt(
                    memberId, periodType.toString(), settlementCycle,
                    startDate, endDate, status
            );
        } else {
            return settlementResponseMapper.searchDefaultCnt(
                    memberId, periodType.toString(), settlementCycle,
                    startDate, endDate, performanceTitle, status
            );
        }
    }

    /**
     * 엑셀 헤더 생성
     */
    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "공연명", "판매금액", "환불금액", "정산대상금액",
                "수수료", "대납금액", "적용수수료율", "정산상태", "정산일시"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    /**
     * 데이터 행 생성
     */
    private void createDataRow(Sheet sheet, int rowIndex, SettlementResponseDto item,
                               PeriodType periodType, ViewType viewType) {
        Row row = sheet.createRow(rowIndex);

        // 정산일시 포맷팅
        String settlementDate = formatSettlementDate(item, periodType, viewType);

        row.createCell(0).setCellValue(item.getPerformanceTitle());
        row.createCell(1).setCellValue(item.getSalesAmount());
        row.createCell(2).setCellValue(item.getRefundAmount());
        row.createCell(3).setCellValue(item.getGrossAmount());
        row.createCell(4).setCellValue(item.getCommission());
        row.createCell(5).setCellValue(item.getNetAmount());
        row.createCell(6).setCellValue(item.getContractCharge() + "%");
        row.createCell(7).setCellValue(item.getStatusName());
        row.createCell(8).setCellValue(settlementDate);
    }

    /**
     * 정산일시 포맷팅
     */
    private String formatSettlementDate(SettlementResponseDto item, PeriodType periodType, ViewType viewType) {
        if (viewType == ViewType.PERFORMANCE) {
            return item.getSettlementDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
        } else {
            if (periodType == PeriodType.WEEKLY) {
                return formatWeeklyCycle(item.getSettlementCycle());
            }
            return item.getSettlementCycle() != null ? item.getSettlementCycle() :
                    item.getSettlementDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
        }
    }

    /**
     * 주간 주차 포맷팅
     */
    private String formatWeeklyCycle(String cycleString) {
        if (cycleString == null) return "";
        String[] parts = cycleString.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + parts[1] + " " + parts[2] + "주차";
        }
        return cycleString;
    }

    /**
     * 엑셀 파일 다운로드
     */
    private void downloadExcelFile(SXSSFWorkbook workbook, HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "_" +
                LocalDate.now() + ".xlsx\"");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        }
    }
}
