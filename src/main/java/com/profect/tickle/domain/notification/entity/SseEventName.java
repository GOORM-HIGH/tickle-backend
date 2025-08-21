package com.profect.tickle.domain.notification.entity;

import lombok.Getter;

@Getter
public enum SseEventName {
    NOTIFICATION("notification"),
    SSE_CONNECT("sse-connect"),
    PING("ping"),
    BYE("bye");

    private final String value;

    SseEventName(String v) {
        this.value = v;
    }
}
