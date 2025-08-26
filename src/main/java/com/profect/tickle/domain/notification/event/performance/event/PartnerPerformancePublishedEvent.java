package com.profect.tickle.domain.notification.event.performance.event;

import com.profect.tickle.domain.performance.dto.response.PerformanceServiceDto;

public record PartnerPerformancePublishedEvent(
        PerformanceServiceDto performance
) {
}
