package com.profect.tickle.domain.performance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceScrapDto {
    private Long performanceId;
    private String title;
    private String img;
    private Instant date;
    private String statusDescription;
    private Instant scrapCreatedAt;
}
