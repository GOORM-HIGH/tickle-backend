package com.profect.tickle.domain.event.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.event.controller.EventController;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.exception.GlobalExceptionHandler;
import com.profect.tickle.global.response.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
class EventServiceImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("중복된 쿠폰 이름이면 예외가 발생한다")
    @Test
    void createCouponEvent_Fail_DuplicateName() throws Exception {
        // Given
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto(
                "중복쿠폰", (short) 100, (short) 10, LocalDate.now().plusDays(7)
        );

        // Mock: 중복 이름일 때 예외 발생
        Mockito.when(eventService.createCouponEvent(Mockito.any()))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_COUPON_NAME));

        // When & Then
        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("이미 존재하는 쿠폰 이름입니다."));
    }

}