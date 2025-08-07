package com.profect.tickle.domain.settlement.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 날짜 유틸
 * @param today
 * @param year YYYY
 * @param month M
 * @param weekOfMonth 입력받은 날의 주차
 * @param dayOfMonth D
 * @param dayOfWeek 요일(월:1 ~ 일:7)
 * @param startOfWeek 월요일의 날짜
 * @param endOfWeek 일요일의 날짜
 */
public record SettlementPeriod(
        LocalDate today,
        int year,
        int month,
        int weekOfMonth,
        int dayOfMonth,
        int dayOfWeek,
        int startOfWeek,
        int endOfWeek
) {
    /**
     * 오늘 또는 파람으로 받은 날짜 기준으로 값 구하기
     * 연도, 월, 주차, 월요일 날짜, 일요일 날짜
     */
    public static SettlementPeriod get(LocalDate now) {
        int y = now.getYear();
        int m = now.getMonthValue();
        int week = weekOfMonth(now);
        int d = now.getDayOfMonth();
        int dow = now.withDayOfMonth(d).getDayOfWeek().getValue();
        int monday = d - dow + 1;
        int sunday = monday + 6;

        return new SettlementPeriod(now, y, m, week, d, dow, monday, sunday);
    }

    /**
     * 입력받은 날짜가 몇주차인지
     */
    public static int weekOfMonth(LocalDate now) {
        // 1일의 요일 (1=월요, 7=일요)
        int firstDow = now.withDayOfMonth(1).getDayOfWeek().getValue();
        // 첫 주의 길이가 1일(월)~7일(일) 중 몇 일부터 시작하는지 보정
        // 예: 1일이 수요(3)이면, offset=2 → (dayOfMonth + offset -1)/7 +1
        int offset = (firstDow - DayOfWeek.MONDAY.getValue() + 7) % 7;
        return (now.getDayOfMonth() + offset - 1) / 7 + 1;
    }
}
