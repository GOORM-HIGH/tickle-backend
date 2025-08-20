package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;

import static com.profect.tickle.global.exception.ErrorCode.INVALID_INPUT_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@DisabledInAotMode
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        given(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Test
    @DisplayName("알림 조회: size 파라미터가 주어지면 해당 개수로 조회한다.")
    @WithMockMember(id = 42, email = "test@tickle.kr")
    void getRecentNotifications_withSizeParam() throws Exception {
        // given
        long memberId = 42L;
        int size = 2;

        given(notificationService.getRecentNotificationListByMemberId(anyLong(), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/notifications")
                .param("size", String.valueOf(size))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        then(notificationService).should()
                .getRecentNotificationListByMemberId(memberId, size);
    }

    @Test
    @DisplayName("알림 조회: size 파라미터 없으면 기본값(10)으로 조회한다.")
    @WithMockMember(id = 42, email = "test@tickle.kr")
    void getRecentNotifications_withoutSizeParam_usesDefault10() throws Exception {
        // given
        long memberId = 42L;
        int size = 10;

        given(notificationService.getRecentNotificationListByMemberId(anyLong(), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/notifications")
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        then(notificationService).should()
                .getRecentNotificationListByMemberId(memberId, size);
    }

    @Test
    @DisplayName("알림 조회: 조회하려는 알림목록의 사이즈가 0 이하이면 400 반환")
    @WithMockMember(id = 42, email = "user@tickle.kr")
    void getRecentNotifications_withNegativeNotificationId_returns400() throws Exception {
        // given
        int size = 0;

        given(notificationService.getRecentNotificationListByMemberId(anyLong(), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/notifications") // 0 또는 음수 → @Positive 위반
                .param("size", String.valueOf(size))
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT_VALUE.getMessage()));
    }

    @Test
    @DisplayName("알림 조회: 조회하려는 알림목록의 사이즈가 11 이상이면 400 반환")
    @WithMockMember(id = 42, email = "user@tickle.kr")
    void getRecentNotifications_sizeOverMax_returns400() throws Exception {
        // given
        int size = 11;

        given(notificationService.getRecentNotificationListByMemberId(anyLong(), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/notifications") // 0 또는 음수 → @Positive 위반
                .param("size", String.valueOf(size))
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT_VALUE.getMessage()));
    }

    @Test
    @DisplayName("알림 읽음 처리: 알림Id를 받고 해당 알림을 읽음 처리 합니다.")
    @WithMockMember(id = 12, email = "test@tickle.kr")
    void markAsRead() throws Exception {
        // given
        long notificationId = 12L;
        long memberId = 12L;

        willDoNothing().given(notificationService)
                .markAsRead(anyLong(), anyLong());

        // when
        ResultActions resultActions = mockMvc.perform(
                patch("/api/v1/notifications/{notificationId}/read", notificationId)
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(204))
                .andExpect(jsonPath("$.message").value(ResultCode.NOTIFICATION_READ_SUCCESS.getMessage()))
        ;

        then(notificationService).should(times(1))
                .markAsRead(notificationId, memberId);
    }

    @Test
    @DisplayName("알림 읽음 처리: 알림 ID가 0 이하이면 400 반환")
    @WithMockMember(id = 1, email = "user@tickle.kr")
    void markAsRead_withNegativeNotificationId_returns400() throws Exception {
        // given
        willDoNothing().given(notificationService)
                .markAsRead(anyLong(), anyLong());

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 0) // 0 또는 음수 → @Positive 위반
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(INVALID_INPUT_VALUE.getMessage()));
    }
}
