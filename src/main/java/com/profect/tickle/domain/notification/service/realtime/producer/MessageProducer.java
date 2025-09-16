package com.profect.tickle.domain.notification.service.realtime.producer;

import com.profect.tickle.domain.notification.dto.NotificationEnvelope;

public interface MessageProducer {

    void produce(String key, NotificationEnvelope<?> envelope);
}
