package com.profect.tickle.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "잘못된 타입 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),
    DATA_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),

    //EVENT
    INVALID_COUPON_VALUE(HttpStatus.BAD_REQUEST, "쿠폰 수량과 할인율은 0 이상이어야 합니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "이미 지난 날짜로 쿠폰을 설정할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
