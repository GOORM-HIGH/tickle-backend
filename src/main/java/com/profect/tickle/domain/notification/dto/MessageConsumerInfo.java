package com.profect.tickle.domain.notification.dto;

public record MessageConsumerInfo(
        String consumerType,      // "redis", "kafka" 등
        String groupId,
        String consumerId,
        String streamKey,
        boolean isRunning,
        long processedMessages,
        long failedMessages,
        long startedAt
) {
}
