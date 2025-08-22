package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RealtimeNotificationController.class)
@DisabledInAotMode
class RealtimeNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RealtimeSender realtimeSender;

    @MockBean
    private ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // 인증 인터셉터 통과
        given(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any()))
                .willReturn(true);
    }

    @Test
    @DisplayName("SSE 연결 - Last-Event-ID 없음 → 200 OK, Content-Type=text/event-stream, connect(12, \"\") 호출")
    @WithMockMember(id = 12, email = "goorm001@goorm.com")
    void connect_success_withoutLastEventId() throws Exception {
        // given
        SseEmitter dummy = new SseEmitter(5_000L);
        given(realtimeSender.connect(eq(12L), eq(""))).willReturn(dummy);

        // when (async 시작)
        MvcResult mvc = mockMvc.perform(
                        get("/api/v1/notifications/connect")
                                .header("Last-Event-ID", "")
                                .accept(MediaType.TEXT_EVENT_STREAM))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        // 응답 커밋 유도
        try {
            dummy.send(SseEmitter.event().name("test").data("ping"));
        } catch (IOException ignored) {
        }
        dummy.complete();

        // then (최종 응답 검증)
        mockMvc.perform(asyncDispatch(mvc))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)));

        then(realtimeSender).should(times(1)).connect(eq(12L), eq(""));
    }

    @Test
    @DisplayName("SSE 연결 - Last-Event-ID 존재 → 200 OK, Content-Type=text/event-stream, connect(34, lastId) 호출")
    @WithMockMember(id = 34, email = "user34@test.com")
    void connect_success_withLastEventId() throws Exception {
        // given
        String lastId = "1724212345678";
        SseEmitter dummy = new SseEmitter(5_000L);
        given(realtimeSender.connect(eq(34L), eq(lastId))).willReturn(dummy);

        // when (async 시작)
        MvcResult mvc = mockMvc.perform(
                        get("/api/v1/notifications/connect")
                                .header("Last-Event-ID", lastId)
                                .accept(MediaType.TEXT_EVENT_STREAM))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        // 응답 커밋 유도
        try {
            dummy.send(SseEmitter.event().name("test").data("ping"));
        } catch (IOException ignored) {
        }
        dummy.complete();

        // then (최종 응답 검증)
        mockMvc.perform(asyncDispatch(mvc))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)));

        then(realtimeSender).should(times(1)).connect(eq(34L), eq(lastId));
    }

    @Test
    @DisplayName("SSE 연결 - Accept 헤더 없어도 Content-Type=text/event-stream 강제 설정")
    @WithMockMember(id = 56, email = "user56@test.com")
    void connect_sets_content_type_even_without_accept_header() throws Exception {
        // given
        SseEmitter dummy = new SseEmitter(5_000L);
        given(realtimeSender.connect(eq(56L), eq(""))).willReturn(dummy);

        // when (async 시작) — Accept 헤더 명시하지 않음
        MvcResult mvc = mockMvc.perform(
                        get("/api/v1/notifications/connect")
                                .header("Last-Event-ID", ""))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        // 응답 커밋 유도
        try {
            dummy.send(SseEmitter.event().name("test").data("ping"));
        } catch (IOException ignored) {
        }
        dummy.complete();

        // then (최종 응답 검증)
        mockMvc.perform(asyncDispatch(mvc))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)));

        then(realtimeSender).should(times(1)).connect(eq(56L), eq(""));
    }

    @Test
    @DisplayName("SSE 연결 - Last-Event-ID 헤더가 완전히 없음 → 컨트롤러가 빈 문자열로 호출")
    @WithMockMember(id = 77, email = "noheader@test.com")
    void connect_success_without_header_at_all() throws Exception {
        // given
        SseEmitter dummy = new SseEmitter(5_000L);
        given(realtimeSender.connect(eq(77L), eq(""))).willReturn(dummy);

        // when (async 시작) — Last-Event-ID 헤더를 아예 넣지 않음
        MvcResult mvc = mockMvc.perform(get("/api/v1/notifications/connect")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 응답 커밋
        try { dummy.send(SseEmitter.event().name("test").data("ping")); } catch (IOException ignored) {}
        dummy.complete();

        // then
        mockMvc.perform(asyncDispatch(mvc))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)));

        then(realtimeSender).should().connect(eq(77L), eq(""));
    }

    // 잘못된 Accept를 보냈을 때는 406/5xx를 기대하는 테스트로 바꿉니다.
    @Test
    @DisplayName("SSE 연결 - Accept=application/json 이면 4xx 반환")
    @WithMockMember(id = 88, email = "json@test.com")
    void connect_with_wrong_accept_header_returns_not_acceptable() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/connect")
                        .header("Last-Event-ID", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("SSE 연결 - realtimeSender.connect()가 예외를 던지면 5xx 반환")
    @WithMockMember(id = 99, email = "error@test.com")
    void connect_when_service_throws_returns_5xx() throws Exception {
        // given
        given(realtimeSender.connect(eq(99L), eq("")))
                .willThrow(new IllegalStateException("boom"));

        // when & then (sync 흐름: asyncStarted 아님)
        mockMvc.perform(get("/api/v1/notifications/connect")
                        .header("Last-Event-ID", ""))
                .andExpect(status().is5xxServerError());

        then(realtimeSender).should().connect(eq(99L), eq(""));
    }

    @Test
    @DisplayName("SSE 연결 - 즉시 complete 되어도 Content-Type=text/event-stream 유지")
    @WithMockMember(id = 66, email = "instant@test.com")
    void connect_immediately_completed_emitter_still_sets_content_type() throws Exception {
        // given
        SseEmitter dummy = new SseEmitter(5_000L);
        given(realtimeSender.connect(eq(66L), eq(""))).willReturn(dummy);

        // when (async 시작)
        MvcResult mvc = mockMvc.perform(get("/api/v1/notifications/connect")
                        .header("Last-Event-ID", "")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 바로 complete
        try { dummy.send(SseEmitter.event().name("hello").data("world")); } catch (IOException ignored) {}
        dummy.complete();

        // then
        mockMvc.perform(asyncDispatch(mvc))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        startsWith(MediaType.TEXT_EVENT_STREAM_VALUE)));

        then(realtimeSender).should().connect(eq(66L), eq(""));
    }

}
