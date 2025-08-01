package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "최신 알림 조회", description = "로그인 사용자의 최신 10건의 알림을 조회합니다.")
    public ResponseEntity<?> getNotificationList() {
        log.info("{}님의 최신 10건의 알림을 조회합니다.", SecurityUtil.getSignInMemberEmail());

        Long signInMemberId = SecurityUtil.getSignInMemberId(); // 로그인한 회원의 Id
        log.info("로그인한 회원 번호:: {}", signInMemberId);

        List<NotificationResponseDto> data = notificationService.getNotificationList(signInMemberId);

        return new ResponseEntity<>(data, HttpStatus.OK);
    }
}
