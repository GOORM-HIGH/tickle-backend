package com.profect.tickle.domain.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceDetailDto {
    private Long performanceId; //공연 ID
    private String title; //공연 제목
    private String img; //공연 이미지
    private Instant date; //공연 일자
    private String StatusDescription;
    private short runtime; //공연 시간
    private boolean isEvent;
    private String price; //공연 금액
    private String hallAddress; //공연장
    private String hostBizName; //주최측
    private Instant startDate; //공연 시작일
    private Instant endDate; //공연 종료일
}
