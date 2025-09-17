package com.profect.tickle.batch.domain.settlement.csvSerializer;

import com.profect.tickle.domain.settlement.entity.SettlementDaily;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.domain.settlement.entity.SettlementMonthly;
import com.profect.tickle.domain.settlement.entity.SettlementWeekly;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

@Component
public class SettlementCsvSerializer {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 건별 정산 CSV 직렬화
     * @param items Writer에서 던진 Chunk 시그니처 items
     * @return String
     */
    public String detailCsvSerializer(Iterable<? extends SettlementDetail> items) {
        // 1) StringBuilder 에 CSV 포맷으로 직렬화
        int capacity = DEFAULT_BUFFER_SIZE;
        if(items instanceof Collection<?> coll){
            capacity = coll.size() * 200;
        }

        StringBuilder sb = new StringBuilder(capacity);
        for(SettlementDetail item : items) {
            // 숫자/문자/타임스탬프를 CSV 규격으로 찍어준다 (쉼표, 개행)
            sb.append(item.getMember().getId()).append(',')
                    .append(item.getStatus().getId()).append(',');
            // 2) performanceTitle (CSV quote 처리)
            appendCsvField(sb, item.getPerformanceTitle());
            sb.append(item.getPerformanceEndDate()).append(',')
                    .append(item.getReservationCode()).append(',')
                    .append(item.getSalesAmount()).append(',')
                    .append(item.getRefundAmount()).append(',')
                    .append(item.getGrossAmount()).append(',')
                    .append(item.getContractCharge()).append(',')
                    .append(item.getCommission()).append(',')
                    .append(item.getNetAmount()).append(',')
                    .append(item.getCreatedAt())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * 일별 정산 CSV 직렬화
     */
    public String dailyCsvSerializer(Iterable<? extends SettlementDaily> items) {
        int capacity = DEFAULT_BUFFER_SIZE;
        if(items instanceof Collection<?> coll){
            capacity = coll.size() * 200;
        }
        StringBuilder sb = new StringBuilder(capacity);

        for(SettlementDaily item : items) {
            sb.append(item.getMember().getId()).append(',')
                    .append(item.getStatus().getId()).append(',');
            appendCsvField(sb, item.getPerformanceTitle());
            sb.append(item.getPerformanceEndDate()).append(',')
                    .append(item.getYear()).append(',')
                    .append(item.getMonth()).append(',')
                    .append(item.getDay()).append(',')
                    .append(item.getDailySalesAmount()).append(',')
                    .append(item.getDailyRefundAmount()).append(',')
                    .append(item.getDailyGrossAmount()).append(',')
                    .append(item.getContractCharge()).append(',')
                    .append(item.getDailyCommission()).append(',')
                    .append(item.getDailyNetAmount()).append(',')
                    .append(item.getDailyCreatedAt())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * 주간 정산 CSV 직렬화
     */
    public String weeklyCsvSerializer(Iterable<? extends SettlementWeekly> items) {
        int capacity = DEFAULT_BUFFER_SIZE;
        if(items instanceof Collection<?> coll){
            capacity = coll.size() * 200;
        }
        StringBuilder sb = new StringBuilder(capacity);

        for(SettlementWeekly item : items) {
            sb.append(item.getMember().getId()).append(',')
                    .append(item.getStatus().getId()).append(',');
            appendCsvField(sb, item.getPerformanceTitle());
            sb.append(item.getYear()).append(',')
                    .append(item.getMonth()).append(',')
                    .append(item.getWeek()).append(',')
                    .append(item.getWeeklySalesAmount()).append(',')
                    .append(item.getWeeklyRefundAmount()).append(',')
                    .append(item.getWeeklyGrossAmount()).append(',')
                    .append(item.getWeeklyCommission()).append(',')
                    .append(item.getWeeklyNetAmount()).append(',')
                    .append(item.getWeeklyCreatedAt())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * 주간 정산 CSV 직렬화
     */
    public String monthlyCsvSerializer(Iterable<? extends SettlementMonthly> items) {
        int capacity = DEFAULT_BUFFER_SIZE;
        if(items instanceof Collection<?> coll){
            capacity = coll.size() * 200;
        }
        StringBuilder sb = new StringBuilder(capacity);

        for(SettlementMonthly item : items) {
            sb.append(item.getMember().getId()).append(',')
                    .append(item.getStatus().getId()).append(',');
            appendCsvField(sb, item.getPerformanceTitle());
            sb.append(item.getYear()).append(',')
                    .append(item.getMonth()).append(',')
                    .append(item.getMonthlySalesAmount()).append(',')
                    .append(item.getMonthlyRefundAmount()).append(',')
                    .append(item.getMonthlyGrossAmount()).append(',')
                    .append(item.getMonthlyCommission()).append(',')
                    .append(item.getMonthlyNetAmount()).append(',')
                    .append(item.getMonthlyCreatedAt())
                    .append('\n');
        }
        return sb.toString();
    }

    /**
     * CSV 필드로 안전하게 변환해서 StringBuilder 에 붙여 준다.
     */
    private void appendCsvField(StringBuilder sb, String field) {
        if (field == null) {
            sb.append("\"\"");    // 빈 값도 "" 로
        } else {
            // 1) 내부 큰따옴표는 "" 로 이스케이프
            String escaped = field.replace("\"", "\"\"");
            // 2) 전체를 "..." 로 감싸서 append
            sb.append('"')
                    .append(escaped)
                    .append('"');
        }
        sb.append(','); // 다음 필드와 구분
    }
}
