package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.config.NonRetryableMailException;
import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final RetryTemplate mailRetryTemplate;

    @Async("mailExecutor")
    @Override
    public void sendText(@Valid MailCreateServiceRequestDto request) {
        mailRetryTemplate.execute(context -> {
            int attempt = context.getRetryCount() + 1;
            log.info("[TEXT] 메일 전송 시도 #{} → to={}", attempt, request.to());

            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(request.to());
                msg.setSubject(request.subject());
                msg.setText(request.content());

                javaMailSender.send(msg);
                log.info("[TEXT] 메일 발송 성공 → to={}", request.to());
                return null;

            } catch (MailException ex) {
                var info = MailErrorInspector.inspect(ex);
                log.warn("[TEXT] 메일 실패(attempt #{}): category={}, status={}, reply={}",
                        attempt, info.category, info.smtpStatus, info.serverReply);
                info.failedByRecipient.forEach(r ->
                        log.warn("  수신자별 실패 addr={}, status={}, reply={}", r.address(), r.smtpStatus(), r.serverReply())
                );

                // 영구 오류(예: 5xx, 주소 거부 등)는 즉시 중단
                if (!isTransient(info)) {
                    throw new NonRetryableMailException("Non-retryable mail error", ex);
                }
                throw ex; // 일시 오류 → RetryTemplate이 백오프 후 재시도
            }
        }, context -> {
            Throwable last = context.getLastThrowable();
            if (last instanceof NonRetryableMailException nre) {
                log.error("[TEXT] 재시도 불가 오류로 중단 → to={}, err={}", request.to(), nre.getCause() != null ? nre.getCause() : nre);
                throw nre; // 필요 시 상위로 전파
            }
            log.error("[TEXT] 최종 실패 → to={}, err={}", request.to(), String.valueOf(last));
            throw new ExhaustedRetryException("메일 발송 최종 실패", last);
        });
    }

    private boolean isTransient(MailErrorInspector.MailErrorInfo info) {
        // 4xx = 일시 오류, 또는 네트워크 계열 카테고리만 재시도
        if ("CONNECT_ERROR".equals(info.category) || "TIMEOUT".equals(info.category)) return true;
        if (info.smtpStatus != null) return info.smtpStatus >= 400 && info.smtpStatus < 500;
        // 상태코드가 없으면 MailSendException 계열만 재시도하게 RetryTemplate이 제한함
        return false;
    }
}
