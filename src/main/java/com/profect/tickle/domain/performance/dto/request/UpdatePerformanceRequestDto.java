package com.profect.tickle.domain.performance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePerformanceRequestDto {
    private String title;
    private Instant date;
    private Short runtime;
    private String img;
    private Boolean isEvent;
}
