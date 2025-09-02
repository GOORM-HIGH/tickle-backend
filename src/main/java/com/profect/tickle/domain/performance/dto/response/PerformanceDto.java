package com.profect.tickle.domain.performance.dto.response;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceDto {
    private Long performanceId;
    private String title;
    private Instant date;
    private String img;
}
