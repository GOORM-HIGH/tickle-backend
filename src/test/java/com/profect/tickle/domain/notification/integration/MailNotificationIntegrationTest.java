package com.profect.tickle.domain.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.config.NonRetryableMailException;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.service.mail.MailErrorInspector;
import com.profect.tickle.domain.notification.service.mail.SmtpMailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.aot.DisabledInAotMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

/**
 * 스프링 컨텍스트에서 SmtpMailSender를 검증하는 통합 테스트.
 */
@SpringBootTest(
        classes = {
                SmtpMailSender.class,
                MailNotificationIntegrationTest.MailTestConfig.class
        },
        properties = {
                // DB/JPA 자동 설정 제외 (메일만 테스트)
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
@DisabledInAotMode // AOT 환경에서 Mockito @MockBean 코드생성 이슈 회피
@DisplayName("[통합] SmtpMailSender")
public class MailNotificationIntegrationTest {

    @MockBean
    JavaMailSender javaMailSender; // 실제 컨텍스트에 주입될 mock

    // ---- 공통 헬퍼
    private static MailErrorInspector.MailErrorInfo info(String category, Integer smtp, String reply) {
        var i = new MailErrorInspector.MailErrorInfo();
        i.category = category;
        i.smtpStatus = smtp;
        i.serverReply = reply;
        i.failedByRecipient = List.of();
        return i;
    }

    @Test
    @DisplayName("[성공] 첫 시도에 정상 발송된다")
    void sendText_successOnFirstAttempt() {
        // given
        willDoNothing().given(javaMailSender).send(any(SimpleMailMessage.class));
        var req = new MailCreateServiceRequestDto("to@ex.com", "subj", "hi");

        // when
        // SmtpMailSender는 스프링 컨텍스트의 RetryTemplate 규칙에 따라 동작
        new SmtpMailSender(javaMailSender, MailTestConfig.fastRetryTemplate()).sendText(req);

        // then
        then(javaMailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("[재시도→성공] 일시 오류(예: 네트워크) 2회 후 3번째 성공한다")
    void sendText_retryThenSucceedOnTransientErrors() {
        // given: 2회 예외, 3번째 정상
        willThrow(new MailSendException("temp-1"))
                .willThrow(new MailSendException("temp-2"))
                .willDoNothing()
                .given(javaMailSender).send(any(SimpleMailMessage.class));

        var req = new MailCreateServiceRequestDto("to@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "transient"));

            // when
            new SmtpMailSender(javaMailSender, MailTestConfig.fastRetryTemplate()).sendText(req);

            // then: 총 3회 호출(2 실패 + 1 성공)
            then(javaMailSender).should(Mockito.times(3)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[즉시중단] 영구 오류(5xx/ADDRESS_REJECTED 등)면 재시도 없이 실패한다")
    void sendText_failFastOnPermanentError() {
        // given: 첫 호출에서 예외
        willThrow(new MailSendException("permanent"))
                .given(javaMailSender).send(any(SimpleMailMessage.class));

        var req = new MailCreateServiceRequestDto("bad@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("ADDRESS_REJECTED", 550, "user unknown"));

            // when / then
            assertThatThrownBy(() ->
                    new SmtpMailSender(javaMailSender, MailTestConfig.fastRetryTemplate()).sendText(req)
            ).isInstanceOf(NonRetryableMailException.class);

            then(javaMailSender).should(Mockito.times(1)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[최종실패] 일시 오류가 계속되면 ExhaustedRetryException을 던진다")
    void sendText_exhaustedAfterAllTransientRetries() {
        // given: 계속 실패
        willThrow(new MailSendException("temp"))
                .given(javaMailSender).send(any(SimpleMailMessage.class));

        var req = new MailCreateServiceRequestDto("to@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "still transient"));

            // when / then
            assertThatThrownBy(() ->
                    new SmtpMailSender(javaMailSender, MailTestConfig.fastRetryTemplate()).sendText(req)
            ).isInstanceOf(org.springframework.retry.ExhaustedRetryException.class);

            then(javaMailSender).should(Mockito.times(3)).send(any(SimpleMailMessage.class));
        }
    }

    // ---- 테스트 전용 구성 (간단/빠른 RetryTemplate 제공)
    static class MailTestConfig {
        @org.springframework.context.annotation.Bean
        RetryTemplate mailRetryTemplate() { // 컨텍스트용
            return fastRetryTemplate();
        }
        static RetryTemplate fastRetryTemplate() { // 테스트 내 new SmtpMailSender()에도 재사용
            return RetryTemplate.builder()
                    .maxAttempts(3)   // 1 + 재시도 2
                    .fixedBackoff(1)  // 거의 즉시
                    .retryOn(MailException.class)
                    .traversingCauses()
                    .build();
        }
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() { // (필요 시 주입되는 경우 대비)
            return new ObjectMapper();
        }
    }
}
