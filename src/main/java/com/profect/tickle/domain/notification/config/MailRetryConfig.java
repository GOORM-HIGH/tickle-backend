package com.profect.tickle.domain.notification.config;

import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.SubclassClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MailRetryConfig {

    @Bean(name = "mailRetryTemplate")
    public RetryTemplate mailRetryTemplate(
            @Value("${mail.retry.max-attempts:4}") int maxAttempts,
            @Value("${mail.retry.delay:500}") long initialDelayMs,
            @Value("${mail.retry.multiplier:2.0}") double multiplier,
            @Value("${mail.retry.max-delay:10000}") long maxDelayMs
    ) {
        // (1) 기본 재시도 정책: 일시 오류만 재시도, cause-traversal 비활성화
        Map<Class<? extends Throwable>, Boolean> retryables = new HashMap<>();
        retryables.put(MailSendException.class, true);
        retryables.put(MailAuthenticationException.class, true);
        retryables.put(MailConnectException.class, true);
        retryables.put(SocketTimeoutException.class, true);
        retryables.put(NonRetryableMailException.class, false); // 안전 차단(기본 정책선에서는 의미 없음)

        SimpleRetryPolicy defaultPolicy =
                new SimpleRetryPolicy(maxAttempts, retryables, /* traverseCauses */ false);

        // (2) NonRetryable 은 아예 NeverRetryPolicy 로 단락
        Map<Class<? extends Throwable>, org.springframework.retry.RetryPolicy> policyMap = new HashMap<>();
        policyMap.put(NonRetryableMailException.class, new NeverRetryPolicy());

        SubclassClassifier<Throwable, org.springframework.retry.RetryPolicy> classifier =
                new SubclassClassifier<>(defaultPolicy); // ← 여기서 "기본 정책"을 지정
        classifier.setTypeMap(policyMap);

        ExceptionClassifierRetryPolicy topPolicy = new ExceptionClassifierRetryPolicy();
        topPolicy.setExceptionClassifier(classifier);

        // (3) 백오프
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(initialDelayMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxDelayMs);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(topPolicy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
