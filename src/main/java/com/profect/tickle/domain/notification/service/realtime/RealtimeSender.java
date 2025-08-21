package com.profect.tickle.domain.notification.service.realtime;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RealtimeSender {

    // 유저의 실시간(SSE) 알림 스트림을 열고, 필요 시 Last-Event-ID 이후 이벤트를 재전송한다.
    SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventId);

    //유저의 모든 emitter(여러 탭)로 브로드캐스트
    void send(long memberId, Object payload);

    // 유저별 이벤트 캐시에서 lastEventId 이후만 재전송
    void resend(long memberId, SseEmitter emitter, String lastEventId);

    // 유저의 모든 emitter를 정상 종료
    void disconnectAll(long memberId);

    // 특정 emitter만 정상 종료
    void disconnectEmitter(long memberId, String emitterId);

    // 특정 emitter를 에러로 종료
    void disconnectEmitterWithError(long memberId, String emitterId, Throwable cause);
}
