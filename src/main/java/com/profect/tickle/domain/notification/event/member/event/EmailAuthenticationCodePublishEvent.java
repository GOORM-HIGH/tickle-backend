package com.profect.tickle.domain.notification.event.member.event;

import com.profect.tickle.domain.notification.dto.request.MailCreateServiceRequestDto;

public record EmailAuthenticationCodePublishEvent(MailCreateServiceRequestDto request) {
}
