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
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."),

    //EVENT
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 정보를 찾을 수 없습니다."),
    EVENT_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이벤트가 진행 중이 아닙니다."),

    //COUPON
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다."),
    ALREADY_ISSUED_COUPON(HttpStatus.CONFLICT, "이미 쿠폰을 발급받았습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 쿠폰을 찾을 수 없습니다."),
    INVALID_COUPON_VALUE(HttpStatus.BAD_REQUEST, "쿠폰 수량과 할인율은 0 이상이어야 합니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "이미 지난 날짜로 쿠폰을 설정할 수 없습니다."),
    DUPLICATE_COUPON_NAME(HttpStatus.CONFLICT, "이미 존재하는 쿠폰 이름입니다."),

    //POINT
    INSUFFICIENT_POINT(HttpStatus.NOT_FOUND, "보유 포인트가 부족합니다."),

    //PERFORMANCE
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연을 찾을 수 없습니다."),
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 장르를 찾을 수 없습니다."),
    DEFAULT_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND,"상태에 기본값이 존재하지 않습니다."),
    PERFORMANCE_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND,"공연 가격이 존재하지 않습니다."),
    NO_PERMISSION(HttpStatus.FORBIDDEN,"공연을 삭제할 권한이 없습니다."),
    ALREADY_SCRAPPED(HttpStatus.CONFLICT,"이미 스크랩된 공연입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND,"스크랩된 공연이 존재하지 않습니다."),

    //SEAT
    SEAT_CLASS_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 등급 정보를 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석 정보를 찾을 수 없습니다."),

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
    MEMBER_UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "회원정보 변경 권한이 없습니다."),

    // 참여자 관련
    CHAT_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방 참여 정보를 찾을 수 없습니다."),

    // 알림 관련
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),
    NOTIFICATION_TEMPLATE_NOT_FOUND(HttpStatus.BAD_REQUEST, "알림 템플릿을 찾을 수 없습니다."),

    // 상태 관련
    STATUS_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "상태를 찾을 수 없습니다."), // 전적인 개발자 잘못: 500 에러

    // 회원 관련
    MEMBER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    VALIDATION_CODE_REQUEST_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "인증번호를 너무 자주 요청했습니다. 잠시 후 다시 시도하세요."),
    VALIDATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    VALIDATION_CODE_MISMATCH(HttpStatus.NOT_FOUND, "인증번호가 일치하지 않습니다."),
    MEMBER_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인만 탈퇴할 수 있습니다."),

    // 예매 관련
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예매 내역을 찾을 수 없습니다."),
    RESERVATION_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "취소할 수 없는 예매입니다."),
    RESERVATION_ALREADY_RESERVED(HttpStatus.CONFLICT, "이미 예매된 좌석이 포함되어 있습니다."),
    RESERVATION_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "총 금액이 일치하지 않습니다."),

    // 좌석 선점 관련
    PREEMPTION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 선점 토큰입니다."),
    PREEMPTION_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "선점 권한이 없습니다."),
    PREEMPTION_EXPIRED(HttpStatus.BAD_REQUEST, "선점 시간이 만료되었습니다."),
    PREEMPTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "좌석은 최대 5개까지 선점할 수 있습니다."),
    PREEMPTION_DUPLICATE_SEAT(HttpStatus.CONFLICT, "이미 선점한 좌석이 포함되어 있습니다."),
    SEAT_PREEMPTION_FAILED(HttpStatus.BAD_REQUEST, "선택한 좌석 중 선점할 수 없는 좌석이 있습니다."),

    // 공연장 유형 관련
    HALL_TYPE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "홀 타입을 찾을 수 없습니다."),
    SEAT_TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "좌석 템플릿을 찾을 수 없습니다."),

    // 정산 배치
    SETTLEMENT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 대상 데이터가 존재하지 않습니다."),
    SETTLEMENT_TARGET_DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "정산 대상 조회 중 DB 오류가 발생했습니다."),
    SETTLEMENT_COMMISSION_CALCULATION_ERROR(HttpStatus.BAD_REQUEST, "수수료 계산 중 오류가 발생했습니다."),
    SETTLEMENT_INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "예매 상태코드가 올바르지 않습니다."),
    SETTLEMENT_INSERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "건별 정산 데이터 저장에 실패했습니다."),
    SETTLEMENT_UPSERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정산 데이터 저장 또는 업데이트에 실패했습니다."),
    SETTLEMENT_STATUS_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "정산 상태 업데이트에 실패했습니다."),

    // 계약 관련
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "계약을 찾지 못했습니다."),
    CONTRACT_CHARGE_INVALID(HttpStatus.BAD_REQUEST, "유요한 수수료율이 아닙니다."),

    ;
    private final HttpStatus status;
    private final String message;
}
