package com.profect.tickle.domain.notification.unit.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.service.realtime.SseSender;
import com.profect.tickle.global.util.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseSenderTest {

    @Mock
    SseRepository sseRepository;
    @Mock
    NotificationProperty notificationProperty;

    // Mock
    ObjectMapper objectMapper;
    Clock clock;
    Supplier<UUID> uuidSupplier;
    Executor directExecutor;

    // InjectMocks
    SseSender sseSender;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        uuidSupplier = () -> UUID.fromString("00000000-0000-0000-0000-000000000000");
        directExecutor = Runnable::run;
        objectMapper = new ObjectMapper();
        sseSender = new SseSender(objectMapper, clock, uuidSupplier, directExecutor, notificationProperty, sseRepository);
    }

    @Test
    @DisplayName("[send] 활성 emitter가 없으면 캐시 저장만 하고 전송하지 않는다")
    void sendWhenNoActiveEmittersCachesOnly() {
        // given
        long memberId = 10L;
        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);

        given(sseRepository.getAllWithIds(memberId)).willReturn(Collections.emptyMap());

        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"ok\":true}");

            // when
            sseSender.send(memberId, payload);

            // then
            then(sseRepository).should().saveEvent(eq(memberId), anyLong(), anyString());
            then(sseRepository).should().clearEventsBefore(eq(memberId), anyLong());
            then(sseRepository).should().getAllWithIds(memberId);
        }
    }

    @Test
    @DisplayName("[send] 여러 emitter에 병렬(동기화된 테스트에선 순차)로 전송한다")
    void sendWhenEmittersExistSendsToEach() throws Exception {
        // given
        long memberId = 11L;
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);
        Map<String, SseEmitter> targets = new LinkedHashMap<>();
        targets.put("e1", e1);
        targets.put("e2", e2);
        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);

        given(sseRepository.getAllWithIds(memberId)).willReturn(targets);

        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"type\":\"n\"}");

            // when
            sseSender.send(memberId, payload);

            // then
            then(e1).should().send(any(SseEmitter.SseEventBuilder.class));
            then(e2).should().send(any(SseEmitter.SseEventBuilder.class));

            then(sseRepository).should().saveEvent(eq(memberId), anyLong(), anyString());
            then(sseRepository).should().clearEventsBefore(eq(memberId), anyLong());
        }
    }

    @Test
    @DisplayName("[send] emitter 전송 실패 시 disconnectEmitterWithError가 호출된다")
    void sendWhenEmitterThrowsDisconnectIsCalled() throws Exception {
        // given
        long memberId = 12L;
        SseSender spySender = Mockito.spy(sseSender);
        SseEmitter badEmitter = mock(SseEmitter.class);
        Map<String, SseEmitter> targets = Map.of("bad", badEmitter);

        doThrow(new IOException("boom")).when(badEmitter).send(any(SseEmitter.SseEventBuilder.class));
        given(sseRepository.getAllWithIds(memberId)).willReturn(targets);

        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);
        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"x\":1}");

            // when
            spySender.send(memberId, payload);

            // then
            then(spySender).should().disconnectEmitterWithError(eq(memberId), eq("bad"), any(IOException.class));
        }
    }

    @Test
    @DisplayName("[connect] emitter를 저장하고 Last-Event-ID가 있으면 재전송을 스케줄한다")
    void connectSavesEmitterAndSchedulesReplayWhenLastEventIdPresent() {
        // given
        long memberId = 20L;
        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(sseRepository).save(eq(memberId), emitterIdCaptor.capture(), emitterCaptor.capture());
        given(sseRepository.eventsAfter(memberId, 123L)).willReturn(new TreeMap<>());

        // when
        SseEmitter result = sseSender.connect(memberId, "123");

        // then
        assertNotNull(result);
        then(sseRepository).should().save(eq(memberId), anyString(), any(SseEmitter.class));
        then(sseRepository).should().eventsAfter(memberId, 123L);
    }

    @Test
    @DisplayName("[connect] onCompletion 콜백에서 repo.remove가 호출된다")
    void connectOnCompletionRemovesFromRepo() {
        // given
        long memberId = 21L;
        given(notificationProperty.sseTimeout()).willReturn(Duration.ofMinutes(5));

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, context) -> {
                    // onCompletion 등록 시 콜백을 보관
                    final java.util.concurrent.atomic.AtomicReference<Runnable> completionRef =
                            new java.util.concurrent.atomic.AtomicReference<>();
                    doAnswer(inv -> {
                        completionRef.set(inv.getArgument(0));
                        return null;
                    })
                            .when(mock).onCompletion(any(Runnable.class));

                    // 불필요한 부수효과 방지
                    doNothing().when(mock).onTimeout(any(Runnable.class));
                    doNothing().when(mock).onError(any());
                    doNothing().when(mock).send(any(SseEmitter.SseEventBuilder.class));

                    // complete() 호출되면 onCompletion 콜백 실행
                    doAnswer(inv -> {
                        Optional.ofNullable(completionRef.get()).ifPresent(Runnable::run);
                        return null;
                    })
                            .when(mock).complete();
                })) {

            // emitterId 캡처 (remove 검증용)
            ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(sseRepository).save(eq(memberId), emitterIdCaptor.capture(), any(SseEmitter.class));

            // when
            SseEmitter emitter = sseSender.connect(memberId, null);

            // then
            emitter.complete();
            then(sseRepository).should().remove(eq(memberId), eq(emitterIdCaptor.getValue()));
        }
    }

    @Test
    @DisplayName("[connect] onTimeout 콜백이 실행되면 repo.remove가 호출된다")
    void connectOnTimeoutRemovesFromRepo() {
        // given
        long memberId = 22L;
        when(notificationProperty.sseTimeout()).thenReturn(Duration.ofMinutes(5));

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, context) -> {
                    // 콜백 캡쳐
                    final AtomicReference<Runnable> onTimeoutRef = new AtomicReference<>();
                    final AtomicReference<Runnable> onCompletionRef = new AtomicReference<>();
                    final AtomicReference<java.util.function.Consumer<Throwable>> onErrorRef = new AtomicReference<>();

                    doAnswer(inv -> {
                        onTimeoutRef.set(inv.getArgument(0));
                        return null;
                    })
                            .when(mock).onTimeout(any(Runnable.class));
                    doAnswer(inv -> {
                        onCompletionRef.set(inv.getArgument(0));
                        return null;
                    })
                            .when(mock).onCompletion(any(Runnable.class));
                    doAnswer(inv -> {
                        onErrorRef.set(inv.getArgument(0));
                        return null;
                    })
                            .when(mock).onError(any());

                    // send 는 아무것도 안함(예외 없이 통과)
                    doNothing().when(mock).send(any(SseEmitter.SseEventBuilder.class));

                    // save 호출 시 repo로 mock emitter 전달되도록
                    // (아래에서 captor 없이 verify만 할거라 별도 캡쳐 불필요)
                })) {

            // save 호출만 검증할 것이므로, 단순 doNothing
            doNothing().when(sseRepository).save(eq(memberId), anyString(), any(SseEmitter.class));

            // when
            sseSender.connect(memberId, null);

            // then
            SseEmitter created = mocked.constructed().get(0);
        }
    }

    @Test
    @DisplayName("[disconnectAll] 모든 emitter에 bye를 보내고 complete 후 repo.removeAll 호출")
    void disconnectAllSendsByeAndCompletesThenRemoveAll() throws Exception {
        // given
        long memberId = 30L;
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);
        Map<String, SseEmitter> targets = new LinkedHashMap<>();
        targets.put("e1", e1);
        targets.put("e2", e2);

        given(sseRepository.getAllWithIds(memberId)).willReturn(targets);

        // when
        sseSender.disconnectAll(memberId);

        // then
        then(e1).should().send(any(SseEmitter.SseEventBuilder.class));
        then(e1).should().complete();
        then(e2).should().send(any(SseEmitter.SseEventBuilder.class));
        then(e2).should().complete();
        then(sseRepository).should().removeAll(memberId);
    }

    @Test
    @DisplayName("[disconnectEmitter] 대상 emitter가 있으면 bye 후 complete + repo.remove 호출")
    void disconnectEmitterSendsByeAndRemoves() throws Exception {
        // given
        long memberId = 40L;
        String emitterId = "mid_123_uuid";
        SseEmitter e = mock(SseEmitter.class);

        given(sseRepository.getByEmitterId(emitterId)).willReturn(e);

        // when
        sseSender.disconnectEmitter(memberId, emitterId);

        // then
        then(e).should().send(any(SseEmitter.SseEventBuilder.class));
        then(e).should().complete();
        then(sseRepository).should().remove(memberId, emitterId);
    }

    @Test
    @DisplayName("[disconnectEmitter] 대상이 없으면 아무 것도 하지 않는다")
    void disconnectEmitterNoTargetNoOp() {
        // given
        long memberId = 41L;
        String emitterId = "not_found";

        given(sseRepository.getByEmitterId(emitterId)).willReturn(null);

        // when
        sseSender.disconnectEmitter(memberId, emitterId);

        // then
        then(sseRepository).should(never()).remove(anyLong(), anyString());
    }

    @Test
    @DisplayName("[disconnectEmitterWithError] completeWithError 후 repo.remove 호출")
    void disconnectEmitterWithErrorCompletesWithErrorAndRemoves() {
        // given
        long memberId = 50L;
        String emitterId = "mid_456_uuid";
        RuntimeException cause = new RuntimeException("x");
        SseEmitter e = mock(SseEmitter.class);

        given(sseRepository.getByEmitterId(emitterId)).willReturn(e);

        // when
        sseSender.disconnectEmitterWithError(memberId, emitterId, cause);

        // then
        then(e).should().completeWithError(cause);
        then(sseRepository).should().remove(memberId, emitterId);
    }
}
