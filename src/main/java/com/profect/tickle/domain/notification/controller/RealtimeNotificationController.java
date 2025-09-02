package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.service.realtime.RealtimeSender;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 스트림이 성공적으로 열렸습니다.", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)), // text/event-stream
            @ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/무효)."),
            @ApiResponse(responseCode = "403", description = "인가 실패(접근 권한 없음)."),
            @ApiResponse(responseCode = "406", description = "클라이언트가 지원하지 않는 Accept 헤더를 보냈습니다. text/event-stream만 허용됩니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {

        log.info("{}님의 실시간 통신 연결 요청이 수신되었습니다.", SecurityUtil.getSignInMemberEmail());

        Long memberId = SecurityUtil.getSignInMemberId();
        SseEmitter emitter = realtimeSender.connect(memberId, lastEventId);

        log.info("{}님의 실시간 통신 연결 요청 처리가 완료되었습니다.", SecurityUtil.getSignInMemberEmail());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-transform")
                .header("X-Accel-Buffering", "no") // NGINX 버퍼링 방지
                .contentType(MediaType.parseMediaType("text/event-stream;charset=UTF-8"))
                .body(emitter);
    }
}
