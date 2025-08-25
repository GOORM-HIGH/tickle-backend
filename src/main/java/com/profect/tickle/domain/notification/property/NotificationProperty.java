package com.profect.tickle.domain.notification.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperty(
        Duration sseTimeout
) {
}
