package com.profect.tickle.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultResponseTest {

    @Test
    @DisplayName("단일 데이터 응답 생성 테스트")
    void createSingleDataResponse() {
        // given
        ResultCode resultCode = ResultCode.RESPONSE_TEST;
        String data = "Single Data Test";

        // when
        ResultResponse<String> response = ResultResponse.of(resultCode, data);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("응답 테스트 성공");
        assertThat(response.getData()).isEqualTo("Single Data Test");
    }

    @Test
    @DisplayName("리스트 데이터 응답 생성 테스트")
    void createListDataResponse() {
        //given
        ResultCode resultCode = ResultCode.RESPONSE_TEST;
        List<String> dataList = List.of("item1", "item2", "item3");

        //when
        ResultResponse<List<String>> response = ResultResponse.of(resultCode, dataList);

        //then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("응답 테스트 성공");
        assertThat(response.getData()).hasSize(3).containsExactly("item1", "item2", "item3");
    }

    @Test
    @DisplayName("리스트 데이터 응답 생성 테스트 - Map 리스트")
    void createListMapDataResponse() {
        // given
        ResultCode resultCode = ResultCode.RESPONSE_TEST;
        List<Map<String, String>> dataList = List.of(
                Map.of("key1", "value1"),
                Map.of("key2", "value2")
        );

        // when
        ResultResponse<List<Map<String, String>>> response = ResultResponse.of(resultCode, dataList);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("응답 테스트 성공");
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0)).containsEntry("key1", "value1");
        assertThat(response.getData().get(1)).containsEntry("key2", "value2");
    }
}