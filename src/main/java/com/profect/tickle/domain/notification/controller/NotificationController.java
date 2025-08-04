package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "최신 알림 조회", description = "로그인 사용자의 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResultResponse<?> getRecentNotificationList(@RequestParam(defaultValue = "10") int size) {
        log.info("{}님의 최신 {}건의 알림을 조회합니다.", SecurityUtil.getSignInMemberEmail(), size);

        Long signInMemberId = SecurityUtil.getSignInMemberId(); // 로그인한 회원의 Id
        List<NotificationResponseDto> data = notificationService.getRecentNotificationListByMemberId(signInMemberId, size);

        return new ResultResponse<>(
                ResultCode.NOTIFICATION_INFO_SUCCESS,
                data
        );
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "읽음 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없는 알림"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        log.info("{}님이 {}번 알림을 읽음 처리합니다.", SecurityUtil.getSignInMemberEmail(), notificationId);

        Long signInMemberId = SecurityUtil.getSignInMemberId(); // 로그인한 회원의 Id
        notificationService.markAsRead(notificationId, signInMemberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "SSE 통신 연결 요청", description = "이벤트 내용을 전달하기 위한 SSE 통신을 연결을 요청합니다.")
    @GetMapping(value = "/sse-connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResultResponse<SseEmitter> connect(@RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") final String lastEventId) {
        return ResultResponse.of(
                ResultCode.SSE_CONNECTION_SUCCESS,
                notificationService.sseConnect(lastEventId)
        );
    }
}
