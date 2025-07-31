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
    INVALID_DATE(HttpStatus.BAD_REQUEST, "이미 지난 날짜로 쿠폰을 설정할 수 없습니다."),

    //POINT
    INSUFFICIENT_POINT(HttpStatus.NOT_FOUND, "보유 포인트가 부족합니다."),

    //SEAT
    SEAT_CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 등급 정보를 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 정보를 찾을 수 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 정보를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."),
    DUPLICATE_COUPON_NAME(HttpStatus.CONFLICT, "이미 존재하는 쿠폰 이름입니다."),

    //PERFORMANCE
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연을 찾을 수 없습니다."),

    // 채팅방 관련
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 공연의 채팅방이 이미 존재합니다."),
    CHAT_ROOM_INACTIVE(HttpStatus.BAD_REQUEST, "비활성화된 채팅방입니다."),
    CHAT_ROOM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "채팅방 정원이 초과되었습니다."),

    // 메시지 관련
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메시지입니다."),
    CHAT_MESSAGE_EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "텍스트 메시지의 내용은 필수입니다."),
    CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지 내용은 255자 이하여야 합니다."),
    CHAT_MESSAGE_MISSING_FILE_INFO(HttpStatus.BAD_REQUEST, "파일 정보는 필수입니다."),
    CHAT_MESSAGE_INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "올바른 파일 크기를 입력해주세요."),
    CHAT_MESSAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 메시지입니다."),

    // 권한 관련
    CHAT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "채팅 기능에 대한 권한이 없습니다."),
    CHAT_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "채팅방에 참여하지 않은 사용자입니다."),
    CHAT_NOT_MESSAGE_OWNER(HttpStatus.FORBIDDEN, "메시지 작성자만 수정/삭제할 수 있습니다."),

    // 참여자 관련
    CHAT_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방 참여 정보를 찾을 수 없습니다."),

    // 알림 관련
//    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),

    // 상태 관련
    STATUS_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "상태를 찾을 수 없습니다."); // 전적인 개발자 잘못: 500 에러

    private final HttpStatus status;
    private final String message;
}
