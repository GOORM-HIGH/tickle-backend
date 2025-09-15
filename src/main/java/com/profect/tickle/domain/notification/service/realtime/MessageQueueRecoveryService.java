package com.profect.tickle.domain.notification.service.realtime;

import java.time.Duration;

public interface MessageQueueRecoveryService {

    void recoverPendingMessages();

    boolean recoverMessage(String messageId);

    boolean discardMessage(String messageId, Duration threshold);
}
