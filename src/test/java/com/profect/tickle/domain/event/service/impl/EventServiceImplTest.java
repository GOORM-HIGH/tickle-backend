/*
package com.profect.tickle.domain.event.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.event.controller.EventController;
import com.profect.tickle.domain.event.dto.request.CouponCreateRequestDto;
import com.profect.tickle.domain.event.dto.response.CouponResponseDto;
import com.profect.tickle.domain.event.entity.Coupon;
import com.profect.tickle.domain.event.entity.Event;
import com.profect.tickle.domain.event.repository.CouponRepository;
import com.profect.tickle.domain.event.repository.EventRepository;
import com.profect.tickle.domain.event.service.EventService;
import com.profect.tickle.domain.reservation.repository.SeatRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
class EventServiceImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private SeatRepository seatRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("쿠폰 이벤트 생성에 성공한다.")
    void createCouponEvent_success() {
        // Given
        Coupon coupon = Coupon.create("쿠폰명", "내용", (short) 10, (short) 20, LocalDate.now().plusDays(5));
        when(couponRepository.existsByName("쿠폰")).thenReturn(false); // 쿠폰명 중복 없음
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        Status mockStatus = Status.create("EVENT", (short) 100, "진행중");
        when(statusRepository.findByTypeAndCode("EVENT", (short) 100)).thenReturn(Optional.of(mockStatus));

        Event event = Event.create(mockStatus , coupon, "쿠폰명");
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        CouponCreateRequestDto request = new CouponCreateRequestDto("쿠폰", "20% 할인", (short) 20, (short) 20, LocalDate.now().plusDays(5));

        // When
        CouponResponseDto response = eventService.createCouponEvent(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponName()).isEqualTo("쿠폰");
    }


    @DisplayName("중복된 쿠폰 이름이면 예외가 발생한다")
    @Test
    void createCouponEvent_Fail_DuplicateName() throws Exception {
        // Given
        CouponCreateRequestDto requestDto = new CouponCreateRequestDto(
                "중복쿠폰", "시원한 여름 맞이 쿠폰", (short) 100, (short) 10, LocalDate.now().plusDays(7)
        );

        // Mock: 중복 이름일 때 예외 발생
        when(eventService.createCouponEvent(any()))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_COUPON_NAME));

        // When & Then
        mockMvc.perform(post("/api/v1/event/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("이미 존재하는 쿠폰 이름입니다."));
    }

}*/
