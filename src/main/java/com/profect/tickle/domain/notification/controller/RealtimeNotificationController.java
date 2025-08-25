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
            // 성공: SSE 스트림 오픈
            @ApiResponse(responseCode = "200", description = "SSE 스트림이 성공적으로 열렸습니다.", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)), // text/event-stream
            // 인증/인가 실패 (보안 필터에서 발생 가능)
            @ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/무효)."),
            @ApiResponse(responseCode = "403", description = "인가 실패(접근 권한 없음)."),
            // 클라이언트가 Accept를 application/json 등으로 보낸 경우 (테스트에서 검증)
            @ApiResponse(responseCode = "406", description = "클라이언트가 지원하지 않는 Accept 헤더를 보냈습니다. text/event-stream만 허용됩니다."),
            // 내부 예외 (서비스/전송 중 예외 등)
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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
