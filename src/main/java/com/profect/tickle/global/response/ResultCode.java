package com.profect.tickle.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResultCode {
    RESPONSE_TEST(HttpStatus.OK, "응답 테스트 성공"),

    //EVENT
    EVENT_INFO_SUCCESS(HttpStatus.OK, "이벤트 조회 성공"),
    EVENT_CREATE_SUCCESS(HttpStatus.CREATED, "이벤트 생성 성공"),
    COUPON_ISSUE_SUCCESS(HttpStatus.OK, "이벤트 쿠폰 지급 성공"),
    COUPON_INFO_SUCCESS(HttpStatus.OK, "쿠폰 조회 성공"),

    //POINT
    POINT_INFO_SUCCESS(HttpStatus.OK, "포인트 조회 성공"),
    POINT_CHARGE_SUCCESS(HttpStatus.OK, "포인트 충전 성공"),
    POINT_HISTORY_SUCCESS(HttpStatus.OK, "포인트 충전/사용 내역 조회 성공"),

    //PERFORMANCE
    GENRE_LIST_SUCCESS(HttpStatus.OK,"장르별 공연 조회 성공"),
    PERFORMANCE_LIST_SUCCESS(HttpStatus.OK,"장르별 공연 조회 성공"),
    PERFORMANCE_DETAIL_SUCCESS(HttpStatus.OK,"공연 상세정보 조회 성공"),
    PERFORMANCE_GENRE_RANK_SUCCESS(HttpStatus.OK,"장르별 인기랭킹 TOP10 조회 성공"),
    PERFORMANCE_TOP100_SUCCESS(HttpStatus.OK,"모든장르포함 인기랭킹 TOP10 조회 성공"),
    PERFORMANCE_POPULAR_SUCCESS(HttpStatus.OK,"예매예정인 TOP4 조회 성공"),
    PERFORMANCE_SEARCH_SUCCESS(HttpStatus.OK,"검색 성공"),
    PERFORMANCE_RECOMMEND_LIST_SUCCESS(HttpStatus.OK,"장르별 공연 조회 성공"),

    // NOTIFICATION
    NOTIFICATION_INFO_SUCCESS(HttpStatus.OK, "알림 조회 성공"),
    NOTIFICATION_READ_SUCCESS(HttpStatus.NO_CONTENT, "알림 읽음 성공"),
    SSE_CONNECTION_SUCCESS(HttpStatus.OK, "SSE 통신 연결 성공"),

    // MEMBER
    MEMBER_CREATE_SUCCESS(HttpStatus.CREATED, "회원가입 성공"),
    EMAIL_VALIDATION_CODE_CREATE(HttpStatus.CREATED, "이메일 인증코드 생성 성공"),
    EMAIL_VERIFICATION_SUCCESS(HttpStatus.OK, "이메일 인증 성공"),

    ;

    private final HttpStatus status;
    private final String message;
}
