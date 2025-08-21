package com.profect.tickle.domain.performance.dto.response;

import java.time.Instant;

public record PerformanceServiceDto(
        Long id,
        Long hostId,
        Long hallId,
        Long genreId,
        Long statusId,
        String title,
        String price,
        Instant performanceDateAndTime,
        Short runtime,
        String thumbnailUrl,
        Instant startDate,
        Instant endDate,
        Boolean isEvent,
        Short lookCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}


