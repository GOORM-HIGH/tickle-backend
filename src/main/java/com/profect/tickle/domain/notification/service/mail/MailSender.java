package com.profect.tickle.domain.notification.service.mail;

import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;
import jakarta.validation.Valid;

public interface MailSender {

    void sendText(@Valid MailCreateServiceRequestDto request);
}
