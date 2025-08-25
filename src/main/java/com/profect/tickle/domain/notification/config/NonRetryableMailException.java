package com.profect.tickle.domain.notification.config;

public class NonRetryableMailException extends RuntimeException {

    public NonRetryableMailException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonRetryableMailException(String msg) {
        super(msg);
    }
}