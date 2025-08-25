package com.profect.tickle.domain.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.notification.config.SseTestConfig;
import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.domain.notification.service.realtime.SseSender;
import com.profect.tickle.global.util.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = {SseSender.class, SseTestConfig.class},
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
@DisplayName("[통합] RealtimeSender(SseSender)")
class RealtimeNotificationIntegrationTest {

    @Autowired RealtimeSender sseSender;
    @Autowired SseRepository sseRepository;

    @Test
    @DisplayName("[connect→replay→send] 재연결 후 새 이벤트까지 전송된다")
    void connectThenReplayThenSendNewEventGoesOut() throws Exception {
        long memberId = 100L;

        try (MockedConstruction<SseEmitter> cons = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> {
                    Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class));
                    Mockito.doNothing().when(mock).send(Mockito.any(), Mockito.any());
                });
             MockedStatic<JsonUtils> st = Mockito.mockStatic(JsonUtils.class)) {

            st.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"x\":1}");

            // when
            SseEmitter emitter = sseSender.connect(memberId, "1000");

            // then
            assertThat(emitter).isNotNull();
            SseEmitter created = cons.constructed().get(0);
            then(created).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));

            // when
            NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);
            sseSender.send(memberId, payload);

            // then
            then(created).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("[send] 다중 emitter에 모두 전송된다")
    void sendToMultipleEmittersSendsToEach() throws Exception {
        long memberId = 200L;

        try (MockedConstruction<SseEmitter> cons = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> {
                    Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class));
                    Mockito.doNothing().when(mock).send(Mockito.any(), Mockito.any());
                });
             MockedStatic<JsonUtils> st = Mockito.mockStatic(JsonUtils.class)) {

            st.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"n\":1}");

            // given: emitter 두 개 연결(테스트 config의 uuidSupplier가 서로 다른 ID 보장)
            sseSender.connect(memberId, null);
            sseSender.connect(memberId, null);
            assertThat(sseRepository.getAllWithIds(memberId)).hasSize(2);

            // when
            NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);
            sseSender.send(memberId, payload);

            // then: 각각 최소 1회 이상 전송
            List<SseEmitter> created = cons.constructed();
            then(created.get(0)).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            then(created.get(1)).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("[send] 한 emitter 전송 실패 시 해당 emitter는 제거되고 나머지는 계속 전송된다")
    void sendWhenOneEmitterThrowsItGetsDisconnectedOthersContinue() throws Exception {
        long memberId = 210L;

        try (MockedConstruction<SseEmitter> cons = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class)));
             MockedStatic<JsonUtils> st = Mockito.mockStatic(JsonUtils.class)) {

            st.when(() -> JsonUtils.toJson(any(ObjectMapper.class), any()))
                    .thenReturn("{\"n\":1}");

            // given
            sseSender.connect(memberId, null);
            sseSender.connect(memberId, null);
            List<SseEmitter> created = cons.constructed();
            SseEmitter bad = created.get(0);
            SseEmitter good = created.get(1);

            // 다음 전송부터 bad가 터지도록
            Mockito.doThrow(new java.io.IOException("boom"))
                    .when(bad).send(any(SseEmitter.SseEventBuilder.class));

            // when
            NotificationEnvelope<?> payload = mock(NotificationEnvelope.class);
            sseSender.send(memberId, payload);

            // then
            then(good).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            assertThat(sseRepository.getAll(memberId).size()).isEqualTo(1); // bad 제거됨
        }
    }

    @Test
    @DisplayName("[disconnectAll] 모든 emitter에 bye를 보내고 complete 후 repo가 비워진다")
    void disconnectAllSendsByeCompletesAndClearsRepo() throws Exception {
        long memberId = 300L;

        try (MockedConstruction<SseEmitter> cons = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> {
                    Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class));
                    Mockito.doNothing().when(mock).complete();
                })) {

            // given
            sseSender.connect(memberId, null);
            sseSender.connect(memberId, null);
            var targets = sseRepository.getAllWithIds(memberId);
            assertThat(targets).hasSize(2);

            // when
            sseSender.disconnectAll(memberId);

            // then: repo에 있던 실제 emitter 인스턴스에 complete() 호출됨
            for (SseEmitter e : targets.values()) {
                then(e).should().complete();
            }
            assertThat(sseRepository.getAll(memberId)).isEmpty();
        }
    }

    @Test
    @DisplayName("[disconnectEmitter] 특정 emitter만 bye/complete 되고 repo에서 제거된다")
    void disconnectEmitterSendsByeAndRemovesOnlyTarget() throws Exception {
        long memberId = 400L;

        try (MockedConstruction<SseEmitter> ignored = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> {
                    Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class));
                    Mockito.doNothing().when(mock).complete();
                })) {

            // given
            sseSender.connect(memberId, null);
            sseSender.connect(memberId, null);
            String anyId = sseRepository.getAllWithIds(memberId).keySet().iterator().next();

            // when
            sseSender.disconnectEmitter(memberId, anyId);

            // then
            assertThat(sseRepository.getAll(memberId).size()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("[disconnectEmitter] 대상이 없으면 아무 동작도 하지 않는다")
    void disconnectEmitterWithMissingIdDoesNothing() {
        long memberId = 410L;
        sseSender.disconnectEmitter(memberId, "no_such_emitter");
        assertThat(sseRepository.getAll(memberId)).isEmpty();
    }

    @Test
    @DisplayName("[connect][onCompletion] 콜백 실행 시 repo에서 제거된다")
    void connectCompletionCallbackRemovesFromRepo() throws Exception {
        long memberId = 500L;

        // onCompletion Runnable을 받아서 나중에 직접 실행하기 위한 저장소
        java.util.List<java.util.concurrent.atomic.AtomicReference<Runnable>> completions = new java.util.ArrayList<>();

        try (MockedConstruction<SseEmitter> cons = Mockito.mockConstruction(
                SseEmitter.class,
                (mock, ctx) -> {
                    // 초기 ping 등 send는 no-op
                    Mockito.doNothing().when(mock).send(Mockito.any(SseEmitter.SseEventBuilder.class));

                    // onCompletion 콜백을 캡처
                    var ref = new java.util.concurrent.atomic.AtomicReference<Runnable>();
                    completions.add(ref);
                    Mockito.doAnswer(inv -> {
                        Runnable r = inv.getArgument(0);
                        ref.set(r);
                        return null;
                    }).when(mock).onCompletion(Mockito.any(Runnable.class));

                    // 나머지 콜백은 테스트에서 사용 안 하므로 no-op
                    Mockito.doNothing().when(mock).onTimeout(Mockito.any(Runnable.class));
                    Mockito.doNothing().when(mock).onError(Mockito.any());
                })) {

            // when: connect → repo에 1개 등록
            sseSender.connect(memberId, null);
            assertThat(sseRepository.getAll(memberId).size()).isEqualTo(1);

            // then: onCompletion을 수동 실행 → repo에서 제거되는지 확인
            Runnable completion = completions.get(0).get();
            assertThat(completion).isNotNull();
            completion.run();

            assertThat(sseRepository.getAll(memberId)).isEmpty();
        }
    }

}
