package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.config.NonRetryableMailException;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SmtpMailSenderTest {

    private JavaMailSender mailSender; // mock
    private SmtpMailSender sut;

    @BeforeEach
    void setUp() {
        mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)      // 1회 + 재시도 2회
                .fixedBackoff(1)     // 최소 1ms
                // 비재시도 예외는 명시적으로 제외 (가독성/안전)
                .notRetryOn(com.profect.tickle.domain.notification.config.NonRetryableMailException.class)
                .build();
        sut = new SmtpMailSender(mailSender, retryTemplate);
    }

    private static MailErrorInspector.MailErrorInfo info(String category, Integer smtp, String reply) {
        MailErrorInspector.MailErrorInfo i = new MailErrorInspector.MailErrorInfo();
        i.category = category;
        i.smtpStatus = smtp;
        i.serverReply = reply;
        i.failedByRecipient = List.of();
        return i;
    }

    @Test
    @DisplayName("[메일 전송][성공] 유효한 입력이면 1회만 전송한다")
    void sendTextSuccess() {
        // given
        MailCreateServiceRequestDto req =
                new MailCreateServiceRequestDto("goorm001@goorm.com", "testsubject", "testcontent");

        // when
        sut.sendText(req);

        // then
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("[메일 전송][재시도] 일시 오류 2회 발생 시 재시도 후 성공한다")
    void shouldRetryAndSucceedWhenTransientErrorOccursTwice() {
        // Given
        willThrow(new MailSendException("temp-1"))
                .willThrow(new MailSendException("temp-2"))
                .willDoNothing()
                .given(mailSender)
                .send(any(SimpleMailMessage.class));

        MailCreateServiceRequestDto req =
                new MailCreateServiceRequestDto("goorm001@goorm.com", "testsubject", "testcontent");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            // 모든 MailException을 "CONNECT_ERROR"(재시도 대상)로 분류
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "transient"));

            // When
            sut.sendText(req);

            // Then: 총 3회 호출(2회 실패 + 1회 성공)
            then(mailSender).should(org.mockito.Mockito.times(3)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[메일 전송][재시도] 영구 오류면 즉시 중단한다")
    void shouldFailFastOnPermanentError() {
        // given: 첫 호출에서 영구 오류 발생
        willThrow(new MailSendException("permanent"))
                .given(mailSender)
                .send(any(SimpleMailMessage.class));

        MailCreateServiceRequestDto req = new MailCreateServiceRequestDto("bad@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            // 영구 오류로 분류 (예: ADDRESS_REJECTED 혹은 5xx)
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("ADDRESS_REJECTED", 550, "user unknown"));

            // when & then
            assertThrows(NonRetryableMailException.class, () -> sut.sendText(req));
            // 재시도 없이 1회만 시도
            then(mailSender).should(times(1)).send(any(SimpleMailMessage.class));
        }
    }

    @Test
    @DisplayName("[메일 전송][재시도] 일시 오류가 계속되면 ExhaustedRetryException을 던진다")
    void shouldThrowExhaustedRetryExceptionWhenTransientErrorsPersist() {
        // given: 매 시도마다 일시 오류
        willThrow(new MailSendException("temp"))
                .given(mailSender)
                .send(any(SimpleMailMessage.class));

        MailCreateServiceRequestDto req =
                new MailCreateServiceRequestDto("to@ex.com", "s", "c");

        try (MockedStatic<MailErrorInspector> mocked = Mockito.mockStatic(MailErrorInspector.class)) {
            mocked.when(() -> MailErrorInspector.inspect(any(MailException.class)))
                    .thenReturn(info("CONNECT_ERROR", null, "still transient"));

            // when & then
            assertThrows(org.springframework.retry.ExhaustedRetryException.class, () -> sut.sendText(req));
            // maxAttempts(3) 만큼 호출
            then(mailSender).should(org.mockito.Mockito.times(3)).send(any(SimpleMailMessage.class));
        }
    }
}
