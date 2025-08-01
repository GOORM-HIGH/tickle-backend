package com.profect.tickle.domain.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceDetailDto {
    private Long performanceId; //공연 ID
    private String title; //공연 제목
    private String img; //공연 이미지
    private LocalDateTime date; //공연 일자
    private String StatusDescription;
    private short runtime; //공연 시간
    private Integer price; //공연 금약
    private String hallAddress; //공연장
    private String hostBizName; //주최측
    private LocalDateTime startDate; //공연 시작일
    private LocalDateTime endDate; //공연 종료일
}
