package com.profect.tickle.domain.notification.integration;

import com.profect.tickle.domain.notification.config.MailRetryConfig;
import com.profect.tickle.domain.notification.config.NonRetryableMailException;
import com.profect.tickle.domain.notification.config.NotificationTestConfig;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import com.profect.tickle.domain.notification.service.mail.MailErrorInspector;
import com.profect.tickle.domain.notification.service.mail.SmtpMailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.aot.DisabledInAotMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest(
        classes = {
                SmtpMailSender.class,
                NotificationTestConfig.class,
                MailRetryConfig.class
        },
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
@DisabledInAotMode
@DisplayName("[통합] SmtpMailSender")
public class MailNotificationIntegrationTest {

    @MockBean
    JavaMailSender mailSender; // 실제 메일 전송 목

    @Autowired
    SmtpMailSender smtpMailSender; // 컨텍스트에서 주입받음(메일 RetryTemplate 포함)

    private static MailErrorInspector.MailErrorInfo info(String category, Integer smtp, String reply) {
        MailErrorInspector.MailErrorInfo info = new MailErrorInspector.MailErrorInfo();
        info.category = category;
        info.smtpStatus = smtp;
        info.serverReply = reply;
        info.failedByRecipient = List.of();
        return info;
    }

    @Test
    @DisplayName("[성공] 첫 시도에 정상 발송된다")
    void sendText_successOnFirstAttempt() {
        // given
        willDoNothing().given(mailSender).send(any(SimpleMailMessage.class));
        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto("to@ex.com", "subj", "hi");

        // when
        smtpMailSender.sendText(req);

        // then
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("[재시도 → 성공] 일시 오류 2회 후 3번째 성공한다")
    void sendText_retryThenSucceedOnTransientErrors() {
        // given
        willThrow(new MailSendException("temp-1"))
                .willThrow(new MailSendException("temp-2"))
                .willDoNothing()
                .given(mailSender).send(any(SimpleMailMessage.class));

        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto("to@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "transient"));

            // when
            smtpMailSender.sendText(req);

            // then
            then(mailSender).should(Mockito.times(3)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[즉시중단] 영구 오류면 재시도 없이 실패한다")
    void sendText_failFastOnPermanentError() {
        // given
        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto("bad@ex.com", "s", "c");

        willThrow(new MailSendException("permanent")).given(mailSender).send(any(SimpleMailMessage.class));

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("ADDRESS_REJECTED", 550, "user unknown"));

            // when & then
            assertThatThrownBy(() -> smtpMailSender.sendText(req))
                    .isInstanceOf(NonRetryableMailException.class);

            then(mailSender).should(Mockito.times(1)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[최종실패] 일시 오류가 계속되면 ExhaustedRetryException")
    void sendText_exhaustedAfterAllTransientRetries() {
        // given
        willThrow(new MailSendException("temp"))
                .given(mailSender).send(any(SimpleMailMessage.class));

        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto("to@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "still transient"));

            // when & then
            assertThatThrownBy(() -> smtpMailSender.sendText(req))
                    .isInstanceOf(org.springframework.retry.ExhaustedRetryException.class);

            then(mailSender).should(Mockito.times(4)).send(any(SimpleMailMessage.class));
        }
    }
}
