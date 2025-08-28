package com.profect.tickle.domain.notification.service.mail;

import jakarta.mail.MessagingException;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;

import java.util.*;
import java.util.stream.Collectors;

public final class MailErrorInspector {

    public static MailErrorInfo inspect(MailException ex) {
        MailErrorInfo info = new MailErrorInfo();

        if (ex instanceof MailSendException mse) {
            info.failedByRecipient = mse.getFailedMessages().entrySet().stream()
                    .flatMap(e -> flattenCauses(e.getValue()).stream()
                            .map(c -> toRecipientError(c)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 최상위 cause 체인에서 대표 사유도 추출
        List<Throwable> causes = flattenCauses(ex);
        for (Throwable t : causes) {
            if (t instanceof SMTPAddressFailedException s) {
                info.smtpStatus = s.getReturnCode();                        // 예: 550
                info.serverReply = s.getMessage();                          // 서버 응답 전체 문자열
                info.category = "ADDRESS_REJECTED";
                break;
            } else if (t instanceof SMTPSendFailedException s) {
                info.smtpStatus = s.getReturnCode();                        // 예: 451
                info.serverReply = s.getMessage();
                info.category = (s.getReturnCode() >= 500) ? "PERMANENT" : "TRANSIENT";
                break;
            } else if (t instanceof MailConnectException) {
                info.category = "CONNECT_ERROR";
                break;
            } else if (t instanceof java.net.SocketTimeoutException) {
                info.category = "TIMEOUT";
                break;
            } else if (t instanceof MessagingException me && me.getNextException() != null) {
                // Jakarta Mail은 nextException에도 중요한 원인이 들어갑니다.
                // loop 상단의 flatten에서 이미 펼쳐집니다.
            }
        }
        return info;
    }

    private static List<Throwable> flattenCauses(Throwable t) {
        List<Throwable> out = new ArrayList<>();
        Set<Throwable> seen = new HashSet<>();
        while (t != null && !seen.contains(t)) {
            out.add(t);
            seen.add(t);
            if (t instanceof MessagingException me && me.getNextException() != null) {
                // MessagingException은 'nextException' 체인도 가짐
                t = me.getNextException();
            } else {
                t = t.getCause();
            }
        }
        return out;
    }

    private static RecipientError toRecipientError(Throwable t) {
        if (t instanceof SMTPAddressFailedException s) {
            return new RecipientError(
                    (s.getAddress() != null) ? s.getAddress().toString() : null,
                    s.getReturnCode(),
                    s.getMessage()
            );
        }
        return null;
    }

    public static class MailErrorInfo {
        public String category;             // 예: TRANSIENT / PERMANENT / CONNECT_ERROR / TIMEOUT / ADDRESS_REJECTED
        public Integer smtpStatus;          // 예: 421/450/451/550/552...
        public String serverReply;          // 서버의 원문 응답 문자열
        public List<RecipientError> failedByRecipient = List.of();
    }

    public static record RecipientError(String address, int smtpStatus, String serverReply) {
    }
}
