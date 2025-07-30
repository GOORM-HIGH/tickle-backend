package com.profect.tickle.domain.chat.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {

    private Integer status;
    private String message;
    private T data;

    // 성공 응답 생성
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
                .status(200)
                .message("성공")
                .data(data)
                .build();
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return ApiResponseDto.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    // 생성 성공 응답
    public static <T> ApiResponseDto<T> created(T data) {
        return ApiResponseDto.<T>builder()
                .status(201)
                .message("생성 성공")
                .data(data)
                .build();
    }

    // 오류 응답 생성
    public static <T> ApiResponseDto<T> error(int status, String message) {
        return ApiResponseDto.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .build();
    }
}
