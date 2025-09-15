package com.profect.tickle.domain.notification.service.realtime;

import com.profect.tickle.domain.notification.dto.NotificationEnvelope;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RealtimeSender {

    // 유저의 실시간알림 스트림을 열고, 필요 시 Last-Event-ID 이후 이벤트를 재전송한다.
    SseEmitter connect(@NotNull Long memberId, @Nullable String lastEventId);

    //유저의 모든 emitter(여러 탭)로 브로드캐스트
    boolean send(long memberId, NotificationEnvelope<?> payload);

    // 모든 유저(접속 여부 상관X)로 브로드캐스트
    boolean sendAll(NotificationEnvelope<?> payload);

    // 유저의 모든 emitter를 정상 종료
    void disconnectAll(long memberId);

    // 특정 emitter만 정상 종료
    void disconnectEmitter(long memberId, String emitterId);
}
