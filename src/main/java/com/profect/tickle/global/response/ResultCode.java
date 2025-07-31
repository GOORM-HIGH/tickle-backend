package com.profect.tickle.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResultCode {
    RESPONSE_TEST(HttpStatus.OK, "응답 테스트 성공"),

    //EVENT
    EVENT_LIST_SUCCESS(HttpStatus.OK, "이벤트 조회 성공"),
    EVENT_CREATE_SUCCESS(HttpStatus.CREATED, "이벤트 생성 성공"),

    //PERFORMANCE
    GENRE_LIST_SUCCESS(HttpStatus.OK,"장르별 공연 조회 성공"),
    PERFORMANCE_LIST_SUCCESS(HttpStatus.OK,"장르별 공연 조회 성공"),
    PERFORMANCE_DETAIL_SUCCESS(HttpStatus.OK,"공연 상세정보 조회 성공"),
    PERFORMANCE_GENRE_RANK_SUCCESS(HttpStatus.OK,"장르별 인기랭킹 TOP10 조회 성공"),
    PERFORMANCE_TOP100_SUCCESS(HttpStatus.OK,"모든장르포함 인기랭킹 TOP10 조회 성공"),
    PERFORMANCE_POPULAR_SUCCESS(HttpStatus.OK,"예매예정인 TOP4 조회 성공");


    private final HttpStatus status;
    private final String message;
}
