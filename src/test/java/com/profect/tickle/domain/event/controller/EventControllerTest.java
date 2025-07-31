/*
package com.profect.tickle.domain.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.exception.GlobalExceptionHandler;
import com.profect.tickle.global.response.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(GlobalExceptionHandler.class)
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 이벤트 생성 성공 테스트")
    void createCouponEvent_Success() throws Exception {
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto(
                "여름 쿠폰", "시원한 여름 맞이 쿠폰",(short) 300, (short) 30, LocalDate.now().plusDays(10)
        );

        CouponResponseDto responseDto = CouponResponseDto.from(
                Coupon.create("여름 쿠폰","시원한 여름 맞이 쿠폰", (short) 300, (short) 30, LocalDate.now().plusDays(10))
        );

        Mockito.when(eventService.createCouponEvent(Mockito.any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ResultCode.EVENT_CREATE_SUCCESS.getStatus().value()))
                .andExpect(jsonPath("$.message").value(ResultCode.EVENT_CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.couponName").value("여름 쿠폰"))
                .andExpect(jsonPath("$.data.couponRate").value(30));
    }

    @DisplayName("쿠폰 수량이 음수이면 예외가 발생한다")
    @Test
    void createCouponEvent_Fail_NegativeCount() throws Exception {
        CouponCreateRequestDto invalidRequest = new CouponCreateRequestDto(
                "봄 쿠폰", "시원한 봄 맞이 쿠폰", (short) -1, (short) 20, LocalDate.now().plusDays(5)
        );

        // When & Then
        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                .andExpect(jsonPath("$.errors[0].field").value("couponCount"))
                .andExpect(jsonPath("$.errors[0].reason").value("쿠폰 수량은 0 이상이어야 합니다."));
    }

    @DisplayName("쿠폰 이름이 비어있으면 예외가 발생한다")
    @Test
    void createCouponEvent_Fail_BlankName() throws Exception {
        // Given
        CouponCreateRequestDto invalidRequest = new CouponCreateRequestDto(
                "", "시원한 봄 맞이 쿠폰",(short) 100, (short) 20, LocalDate.now().plusDays(5)
        );

        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                .andExpect(jsonPath("$.errors[0].field").value("couponName"))
                .andExpect(jsonPath("$.errors[0].reason").value("쿠폰 이름은 2자 이상이어야 합니다."));
    }

    @DisplayName("쿠폰 유효기간이 과거거나 당일이여서는 안된다.")
    @Test
    void createCouponEvent_Fail_DayValid() throws Exception {
        // Given
        CouponCreateRequestDto invalidRequest = new CouponCreateRequestDto(
                "여름 쿠폰","시원한 여름 맞이 쿠폰", (short) 100, (short) 20, LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                .andExpect(jsonPath("$.errors[0].field").value("couponValid"))
                .andExpect(jsonPath("$.errors[0].reason").value("과거, 당일은 유효기간으로 지정될 수 없습니다."));
    }

}*/
