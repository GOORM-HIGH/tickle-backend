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
public class PerformanceHostDto {
    private Long performanceId;
    private String title;
    private Instant date;
    private String img;
    private String statusDescription;
    private Integer lookCount;
    private Instant createdDate;
    private Instant deletedDate;
}
