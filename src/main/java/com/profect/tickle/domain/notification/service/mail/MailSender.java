package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;

public interface MailSender {

    void sendText(MailCreateServiceRequestDto request);

    void sendHtml(MailCreateServiceRequestDto request);
}
