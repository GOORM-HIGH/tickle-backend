package com.profect.tickle.global.security.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExceptionResponseDto {

    private int statusCode;
    private String message;

    public ExceptionResponseDto(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
