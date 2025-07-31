package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "최신 알림 조회", description = "로그인 사용자의 최신 10건의 알림을 조회합니다.")
    public ResponseEntity<?> getNotificationList() {
        List<NotificationResponseDto> data = notificationService.getNotificationList(2L);

        return new ResponseEntity<>(data, HttpStatus.OK);
    }
}
