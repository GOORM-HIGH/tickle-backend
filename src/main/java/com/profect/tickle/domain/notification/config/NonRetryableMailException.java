package com.profect.tickle.domain.notification.config;

public class NonRetryableMailException extends RuntimeException {
    public NonRetryableMailException(String msg, Throwable cause) { super(msg, cause); }
    public NonRetryableMailException(String msg) { super(msg); }
}