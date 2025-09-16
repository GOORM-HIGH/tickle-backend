package com.profect.tickle.domain.notification.service.realtime.consumer;

import com.profect.tickle.domain.notification.dto.MessageConsumerInfo;

public interface MessageConsumer {

    void start();

    void stop();

    boolean isRunning();

    MessageConsumerInfo getInfo();
}