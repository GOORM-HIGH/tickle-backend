package com.profect.tickle.domain.event.entity;

import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum EventType {
    COUPON(0),
    TICKET(1);

    private final int code;

    EventType(int code) {
        this.code = code;
    }

    public static EventType fromCode(int code) {
        for (EventType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_TYPE_VALUE);
    }
}