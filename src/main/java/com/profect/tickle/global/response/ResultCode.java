package com.profect.tickle.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResultCode {
    RESPONSE_TEST(HttpStatus.OK, "응답 테스트 성공");

    private final HttpStatus status;
    private final String message;
}
