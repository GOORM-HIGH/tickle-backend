package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.notification.service.NotificationService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("알림 조회: size 파라미터가 주어지면 해당 개수로 조회한다")
    @WithMockMember(id = 42, email = "test@tickle.kr", roles = "MEMBER")
    void getRecentNotifications_withSizeParam() throws Exception {
        long memberId = 42L;
        int size = 2;

        given(notificationService.getRecentNotificationListByMemberId(memberId, size))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/notifications")
                        .param("size", String.valueOf(size))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(notificationService).getRecentNotificationListByMemberId(memberId, size);
    }

    @Test
    @DisplayName("알림 조회: size 파라미터 없으면 기본값(10)으로 조회한다")
    @WithMockMember(id = 42, email = "test@tickle.kr", roles = "MEMBER")
    void getRecentNotifications_withoutSizeParam_usesDefault10() throws Exception {
        long memberId = 42L;
        int defaultSize = 10;

        when(notificationService.getRecentNotificationListByMemberId(memberId, defaultSize))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(notificationService).getRecentNotificationListByMemberId(memberId, defaultSize);
    }
}
