package com.profect.tickle.domain.notification.config;

import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.retry.support.RetryTemplate;

import java.net.SocketTimeoutException;

@Configuration
public class MailRetryConfig {

    @Bean
    public RetryTemplate mailRetryTemplate(
            @Value("${mail.retry.max-attempts:4}") int maxAttempts,
            @Value("${mail.retry.delay:500}") long initialDelayMs,
            @Value("${mail.retry.multiplier:2.0}") double multiplier,
            @Value("${mail.retry.max-delay:10000}") long maxDelayMs
    ) {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)                       // 초기 1회 + 재시도 (maxAttempts-1)
                .exponentialBackoff(initialDelayMs, multiplier, maxDelayMs) // 지수 백오프
                // 재시도 대상(일반/네트워크 계열). 나머지는 코드에서 NonRetryable로 전환
                .retryOn(MailSendException.class)
                .retryOn(MailAuthenticationException.class)
                .retryOn(MailConnectException.class)
                .retryOn(SocketTimeoutException.class)
                .build();
    }
}
