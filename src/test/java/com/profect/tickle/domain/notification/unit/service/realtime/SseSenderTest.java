package com.profect.tickle.domain.notification.unit.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.property.NotificationProperty;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.service.realtime.SseSender;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.util.JsonUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SseSenderTest {

    @Mock
    SseRepository sseRepository;

    @Mock
    NotificationProperty notificationProperty;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    MeterRegistry meterRegistry;

    // 실제 구현을 주입해서 사용
    Clock clock;
    Supplier<UUID> uuidSupplier;
    Executor directExecutor;

    SseSender sseSender;

    // 상수
    private static final int EXPECTED_MAX_REPLAY = 50;
    private static final long EXPECTED_TTL_MS = java.util.concurrent.TimeUnit.MINUTES.toMillis(10);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        uuidSupplier = () -> UUID.fromString("00000000-0000-0000-0000-000000000000");
        directExecutor = Runnable::run;

        // MeterRegistry Mock 메트릭 관련 자동 응답 셋업
        Counter.Builder mockCounterBuilder = mock(Counter.Builder.class);
        Gauge.Builder mockGaugeBuilder = mock(Gauge.Builder.class);
        Counter mockCounter = mock(Counter.class);
        Gauge mockGauge = mock(Gauge.class);

        when(Counter.builder(anyString())).thenReturn(mockCounterBuilder);
        when(mockCounterBuilder.description(anyString())).thenReturn(mockCounterBuilder);
        when(mockCounterBuilder.register(any(MeterRegistry.class))).thenReturn(mockCounter);
        when(Gauge.builder(anyString(), any())).thenReturn(mockGaugeBuilder);
        when(mockGaugeBuilder.description(anyString())).thenReturn(mockGaugeBuilder);
        when(mockGaugeBuilder.register(any(MeterRegistry.class))).thenReturn(mockGauge);

        sseSender = new SseSender(objectMapper, clock, uuidSupplier, directExecutor,
                notificationProperty, sseRepository, meterRegistry);
    }

    @Test
    @DisplayName("[send] 활성 emitter가 없으면 캐시 저장만 하고 전송하지 않는다")
    void sendWhenNoActiveEmittersCachesOnly() {
        long memberId = 10L;
        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);

        // 활성 emitter 없음
        given(sseRepository.getAllWithIds(memberId)).willReturn(Collections.emptyMap());

        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"ok\":true}");

            // when
            sseSender.send(memberId, payload);

            // then
            then(sseRepository).should(times(1)).getAllWithIds(memberId);
            then(sseRepository).should(times(1))
                    .saveEvent(eq(memberId), anyLong(), eq("{\"ok\":true}"));
            then(sseRepository).should(times(1))
                    .trimEvents(eq(memberId), anyInt(), anyLong());
        }
    }

    @Test
    @DisplayName("[send] 여러 emitter에 전송하고 캐시 저장/트리밍 1회 호출")
    void sendWhenEmittersExistSendsToEach() throws Exception {
        // given
        long memberId = 11L;
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);
        Map<String, SseEmitter> targets = new LinkedHashMap<>();
        targets.put("e1", e1);
        targets.put("e2", e2);

        given(sseRepository.getAllWithIds(memberId)).willReturn(targets);

        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);
        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"type\":\"n\"}");

            // when
            sseSender.send(memberId, payload);

            // then - 각 emitter로 1회 전송
            then(e1).should().send(any(SseEmitter.SseEventBuilder.class));
            then(e2).should().send(any(SseEmitter.SseEventBuilder.class));

            ArgumentCaptor<Long> eventIdCap = ArgumentCaptor.forClass(Long.class);
            then(sseRepository).should(times(1)).saveEvent(eq(memberId), eventIdCap.capture(), eq("{\"type\":\"n\"}"));

            long eventId = eventIdCap.getValue();

            then(sseRepository).should(times(1))
                    .trimEvents(eq(memberId), eq(EXPECTED_MAX_REPLAY), eq(eventId - EXPECTED_TTL_MS));
        }
    }

    @Test
    @DisplayName("[send] emitter 전송 실패 시 disconnectEmitterWithError 호출")
    void sendWhenEmitterThrowsDisconnectIsCalled() throws Exception {
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

            spySender.send(memberId, payload);

            then(spySender).should()
                    .disconnectEmitterWithError(eq(memberId), eq("bad"), any(IOException.class));
        }
    }

    @Test
    @DisplayName("[send] 직렬화 실패 시 BusinessException 발생, 저장/전송 모두 생략")
    void sendWhenSerializationFails() {
        // given
        long memberId = 13L;
        NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);

        try (MockedStatic<JsonUtils> mocked = Mockito.mockStatic(JsonUtils.class)) {
            // JsonUtils.toJson 이 런타임 예외를 던지도록 시뮬레이션
            mocked.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenThrow(new RuntimeException("ser fail"));

            // when & then
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> sseSender.send(memberId, payload)
            );

            assertEquals(ErrorCode.REALTIME_NOTIFICATION_SEND_FAILED, ex.getErrorCode());
            assertEquals(ErrorCode.REALTIME_NOTIFICATION_SEND_FAILED.getMessage(), ex.getMessage());

            // 저장/트리밍/조회 등 레포지토리 상호작용이 전혀 없어야 함
            then(sseRepository).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("[connect] emitter를 저장하고 Last-Event-ID가 있으면 캐시 이벤트를 replay로 전송")
    void connectSavesEmitterAndReplaysWhenLastEventIdPresent() throws Exception {
        // given
        long memberId = 20L;

        given(notificationProperty.sseTimeout()).willReturn(Duration.ofMinutes(5));

        // 캐시 이벤트 준비(2건)
        TreeMap<Long, String> cached = new TreeMap<>();
        cached.put(124L, "{\"type\":\"A\"}");
        cached.put(125L, "{\"type\":\"B\"}");
        given(sseRepository.eventsAfter(memberId, 123L)).willReturn(cached);

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, context) -> {
                    doNothing().when(mock).onCompletion(any());
                    doNothing().when(mock).onTimeout(any());
                    doNothing().when(mock).onError(any());
                    doNothing().when(mock).send(any(SseEmitter.SseEventBuilder.class));
                })) {

            // save 시 emitterId 캡처
            ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
            ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(sseRepository).save(eq(memberId), emitterIdCaptor.capture(), emitterCaptor.capture());

            // when
            SseEmitter result = sseSender.connect(memberId, "123");

            // then
            assertNotNull(result);
            then(sseRepository).should().save(eq(memberId), anyString(), any(SseEmitter.class));
            then(sseRepository).should().eventsAfter(memberId, 123L);

            SseEmitter created = mocked.constructed().get(0);
            // replay 로 2건 전송
            then(created).should(times(2)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("[connect] onCompletion 콜백에서 repo.remove 호출")
    void connectOnCompletionRemovesFromRepo() {
        // given
        long memberId = 21L;

        given(notificationProperty.sseTimeout()).willReturn(Duration.ofMinutes(5));

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, context) -> {
                    final AtomicReference<Runnable> completionRef = new AtomicReference<>();
                    doAnswer(inv -> {
                        completionRef.set(inv.getArgument(0));
                        return null;
                    }).when(mock).onCompletion(any(Runnable.class));

                    doNothing().when(mock).onTimeout(any());
                    doNothing().when(mock).onError(any());
                    doNothing().when(mock).send(any(SseEmitter.SseEventBuilder.class));

                    // when
                    doAnswer(inv -> {
                        Optional.ofNullable(completionRef.get()).ifPresent(Runnable::run);
                        return null;
                    }).when(mock).complete();
                })) {

            // then
            ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(sseRepository).save(eq(memberId), emitterIdCaptor.capture(), any(SseEmitter.class));

            SseEmitter emitter = sseSender.connect(memberId, null);

            emitter.complete();
            then(sseRepository).should().remove(eq(memberId), eq(emitterIdCaptor.getValue()));
        }
    }

    @Test
    @DisplayName("[connect] onTimeout 콜백 실행 시 repo.remove 호출")
    void connectOnTimeoutRemovesFromRepo() {
        // when
        long memberId = 22L;

        given(notificationProperty.sseTimeout()).willReturn(Duration.ofMinutes(5));

        try (MockedConstruction<SseEmitter> mocked = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, context) -> {
                    final AtomicReference<Runnable> onTimeoutRef = new AtomicReference<>();
                    doAnswer(inv -> {
                        onTimeoutRef.set(inv.getArgument(0));
                        return null;
                    }).when(mock).onTimeout(any(Runnable.class));
                    doNothing().when(mock).onCompletion(any());
                    doNothing().when(mock).onError(any());
                    doNothing().when(mock).send(any(SseEmitter.SseEventBuilder.class));
                })) {

            ArgumentCaptor<String> emitterIdCaptor = ArgumentCaptor.forClass(String.class);
            doNothing().when(sseRepository).save(eq(memberId), emitterIdCaptor.capture(), any(SseEmitter.class));

            // when
            sseSender.connect(memberId, null);

            // then
            SseEmitter created = mocked.constructed().get(0);
            ArgumentCaptor<Runnable> timeoutCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(created).onTimeout(timeoutCaptor.capture());

            timeoutCaptor.getValue().run();

            verify(sseRepository).remove(eq(memberId), eq(emitterIdCaptor.getValue()));
        }
    }

    @Test
    @DisplayName("[disconnectAll] 일부 전송 실패해도 모두 complete 후 removeAll")
    void disconnectAllSendsByeAndCompletesThenRemoveAllEvenIfOneFails() throws Exception {
        // given
        long memberId = 30L;
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        // e1 은 send 실패, e2 는 성공
        doThrow(new IOException("nope")).when(e1).send(any(SseEmitter.SseEventBuilder.class));

        Map<String, SseEmitter> targets = new LinkedHashMap<>();
        targets.put("e1", e1);
        targets.put("e2", e2);

        given(sseRepository.getAllWithIds(memberId)).willReturn(targets);

        // when
        sseSender.disconnectAll(memberId);

        // then
        then(e1).should().send(any(SseEmitter.SseEventBuilder.class));
        then(e1).should().complete(); // 실패해도 complete 시도
        then(e2).should().send(any(SseEmitter.SseEventBuilder.class));
        then(e2).should().complete();
        then(sseRepository).should().removeAll(memberId);
    }

    @Test
    @DisplayName("[disconnectEmitter] 대상 emitter가 있으면 bye 후 complete + repo.remove")
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
    @DisplayName("[disconnectEmitter] 대상이 없으면 아무 것도 하지 않음")
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
