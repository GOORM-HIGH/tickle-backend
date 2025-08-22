package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(value = "/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationController {

    private final RealtimeSender realtimeSender;

    @Operation(summary = "실시간통신 연결 요청", description = "이벤트 내용을 전달하기 위한 신시간통신을 연결합니다.")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {

        Long memberId = SecurityUtil.getSignInMemberId();
        SseEmitter emitter = realtimeSender.connect(memberId, lastEventId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
