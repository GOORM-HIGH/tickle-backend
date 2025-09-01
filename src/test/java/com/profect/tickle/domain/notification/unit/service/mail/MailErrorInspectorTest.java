package com.profect.tickle.domain.notification.unit.service.mail;

import com.profect.tickle.domain.notification.service.mail.MailErrorInspector;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.util.MailConnectException;
import org.eclipse.angus.mail.util.SocketConnectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailErrorInspectorTest {

    @Test
    @DisplayName("MailSendException.failedMessages에 들어있는 SMTPAddressFailedException을 수신자별로 파싱한다")
    void inspectFailedMessagesRecipientErrors() throws Exception {
        // given: MessagingException -> nextException 으로 SMTPAddressFailedException 보유
        InternetAddress bad = new InternetAddress("bad@example.com");
        SMTPAddressFailedException smtpAddressFailed =
                new SMTPAddressFailedException(bad, "RCPT", 550, "user unknown");

        MessagingException msgEx = new MessagingException("wrap");
        msgEx.setNextException(smtpAddressFailed); // flattenCauses에서 nextException을 타도록

        Map<Object, Exception> failed = new HashMap<>();
        failed.put("message-1", msgEx);

        MailSendException ex = new MailSendException(failed);

        // when
        MailErrorInspector.MailErrorInfo info = MailErrorInspector.inspect(ex);

        // then
        assertNotNull(info);
        assertNotNull(info.failedByRecipient);
        assertEquals(1, info.failedByRecipient.size());

        MailErrorInspector.RecipientError re = info.failedByRecipient.get(0);
        assertEquals("bad@example.com", re.address());
        assertEquals(550, re.smtpStatus());
        assertTrue(re.serverReply().contains("user unknown"));

        // category는 최상위 cause 체인에 없으므로(null 허용)
        assertNull(info.category);
        assertNull(info.smtpStatus);
        assertNull(info.serverReply);
    }

    @Test
    @DisplayName("SMTPSendFailedException(451)이 cause면 TRANSIENT 카테고리와 상태 코드가 세팅된다")
    void inspectSmtpSendFailedTransient() {
        SMTPSendFailedException cause =
                new SMTPSendFailedException("RCPT", 451, "Try later", null,
                        null, null, null);
        MailSendException ex = new MailSendException("send failed", cause);

        MailErrorInspector.MailErrorInfo info = MailErrorInspector.inspect(ex);

        assertEquals("TRANSIENT", info.category);
        assertEquals(451, info.smtpStatus);
        assertNotNull(info.serverReply);
    }

    @Test
    @DisplayName("SMTPAddressFailedException(550)이 cause면 ADDRESS_REJECTED 카테고리와 상태 코드가 세팅된다")
    void inspectAddressRejected() throws Exception {
        SMTPAddressFailedException cause = new SMTPAddressFailedException(
                new InternetAddress("nope@domain.test"), "RCPT", 550, "Mailbox unavailable");
        MailSendException ex = new MailSendException("send failed", cause);

        MailErrorInspector.MailErrorInfo info = MailErrorInspector.inspect(ex);

        assertEquals("ADDRESS_REJECTED", info.category);
        assertEquals(550, info.smtpStatus);
        assertNotNull(info.serverReply);
    }

    @Test
    @DisplayName("SocketTimeoutException이 cause면 TIMEOUT 카테고리로 분류한다")
    void inspectTimeout() {
        MailSendException ex = new MailSendException("send failed", new SocketTimeoutException("read timed out"));

        MailErrorInspector.MailErrorInfo info = MailErrorInspector.inspect(ex);

        assertEquals("TIMEOUT", info.category);
        assertNull(info.smtpStatus); // 상태 코드는 없음
    }

    @Test
    @DisplayName("MailConnectException이 cause면 CONNECT_ERROR 카테고리로 분류한다")
    void inspectConnectError() {
        SocketConnectException socketEx =
                new SocketConnectException(
                        "smtp.test",
                        new ConnectException("Connection refused"),
                        "Connection refused",
                        25,
                        10000
                );
        MailConnectException cause = new MailConnectException(socketEx);
        MailSendException ex = new MailSendException("send failed", cause);

        MailErrorInspector.MailErrorInfo info = MailErrorInspector.inspect(ex);

        assertEquals("CONNECT_ERROR", info.category);
        assertNull(info.smtpStatus);
    }
}
