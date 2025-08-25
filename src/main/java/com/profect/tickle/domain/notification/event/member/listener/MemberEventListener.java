package com.profect.tickle.domain.notification.event.member.listener;

import com.profect.tickle.domain.notification.event.member.event.EmailAuthenticationCodePublishEvent;
import com.profect.tickle.domain.notification.service.mail.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberEventListener {

    private final MailSender mailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMailSendRequested(EmailAuthenticationCodePublishEvent event) {
        try {
            mailSender.sendText(event.request());
            log.info("메일 전송 요청 처리 완료: {}", event.request().to());
        } catch (Exception e) {
            log.error("메일 전송 실패: {}", event.request().to(), e);
        }
    }
}
