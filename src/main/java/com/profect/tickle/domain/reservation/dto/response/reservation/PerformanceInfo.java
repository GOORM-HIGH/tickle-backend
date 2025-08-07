package com.profect.tickle.domain.reservation.dto.response.reservation;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PerformanceInfo {
    private Long performanceId;
    private String title;                       // 공연명
    private String hall;                       // 공연장
    private Instant performanceDate;      // 공연 일시
    private String posterUrl;                   // 포스터 이미지 URL
    private Short runtime;                   // 공연 시간 (분)
}
