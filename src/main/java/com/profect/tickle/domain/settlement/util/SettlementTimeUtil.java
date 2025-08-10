package com.profect.tickle.domain.settlement.util;

import java.time.*;
import java.time.temporal.ChronoUnit;

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
public record SettlementTimeUtil(
        LocalDate today,
        int year,
        String yearStr,
        int month,
        String monthStr,
        int weekOfMonth,
        String weekOfMonthStr,
        int dayOfMonth,
        String dayOfMonthStr,
        int dayOfWeek,
        int startOfWeek,
        int endOfWeek
) {
    /**
     * 오늘 또는 파람으로 받은 날짜 기준으로 값 구하기
     * 연도, 월, 주차, 월요일 날짜, 일요일 날짜
     */
    public static SettlementTimeUtil get(LocalDate now) {
        int y = now.getYear();
        String yStr = String.valueOf(y);
        int m = now.getMonthValue();
        String mStr = String.format("%02d", m);
        int week = weekOfMonth(now);
        String weekStr = String.format("%02d", week);
        int d = now.getDayOfMonth();
        String dStr = String.format("%02d", d);
        int dow = now.withDayOfMonth(d).getDayOfWeek().getValue();
        int monday = d - dow + 1;
        int sunday = monday + 6;

        return new SettlementTimeUtil(now, y, yStr, m, mStr, week, weekStr,
                d, dStr, dow, monday, sunday);
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

    /**
     * 00시 00분일 때, 23시59분59초.999 반환
     */
    public static Instant getEndOfDay() {
        ZoneId zone = ZoneId.of("Asia/Seoul");

        ZonedDateTime startOfDay = LocalDate.now().atStartOfDay().atZone(zone);

        ZonedDateTime endOfDay = startOfDay.minus(1, ChronoUnit.MILLIS);

        return endOfDay.toInstant();
    }

    /**
     * 현재 시간이 00시 00분인지
     */
    public static boolean isMidnight(Instant now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");

        LocalTime time = now.atZone(zone).toLocalTime();

        return time.equals(LocalTime.MIDNIGHT);
    }

    /**
     * Instant -> LocalDate
     */
    public static LocalDate localDate(Instant now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");

        LocalDate date = now.atZone(zone).toLocalDate();

        return date;
    }
}
