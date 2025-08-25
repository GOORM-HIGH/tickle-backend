package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.config.NonRetryableMailException;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

// SmtpMailSender.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpMailSender {

    private final JavaMailSender mailSender;

    @Qualifier("mailRetryTemplate")
    private final RetryTemplate retryTemplate;

    public void sendText(MailCreateServiceRequestDto req) {
        try {
            retryTemplate.execute(ctx -> {
                log.info("[TEXT] 메일 전송 시도 #{} → to={}", ctx.getRetryCount() + 1, req.to());

                try {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setTo(req.to());
                    msg.setSubject(req.subject());
                    msg.setText(req.content());
                    mailSender.send(msg);
                    return null;

                } catch (MailException ex) {
                    var info = MailErrorInspector.inspect(ex);

                    boolean permanent =
                            (info.smtpStatus != null && info.smtpStatus >= 500) ||
                                    "ADDRESS_REJECTED".equals(info.category) ||
                                    "USER_UNKNOWN".equals(info.category);

                    if (permanent) {
                        log.info("[TEXT] 재시도 불가 오류로 중단 → to={}, err={}", req.to(), info);
                        // 즉시 NonRetryable 던져서 try 블록 바깥으로 전파 (재시도 X)
                        throw new NonRetryableMailException("Permanent mail error: " + info.category, ex);
                    }

                    log.info("[TEXT] 메일 실패(attempt #{}): category={}, status={}, reply={}",
                            ctx.getRetryCount() + 1, info.category, info.smtpStatus, info.serverReply);
                    // 일시 오류 → RetryTemplate이 재시도
                    throw ex;
                }
            });

        } catch (NonRetryableMailException e) {
            // 영구 오류는 그대로 테스트 기대대로 전파
            throw e;

        } catch (MailException exhausted) {
            // 재시도 대상(일시 오류)이 계속 실패하여 소진된 경우만 여기로 온다.
            throw new org.springframework.retry.ExhaustedRetryException("Mail send exhausted", exhausted);
        }
    }
}