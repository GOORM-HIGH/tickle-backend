package com.profect.tickle.global.security.dto.request;

public record SignInRequestDto(
        String email,
        String password) {
}
