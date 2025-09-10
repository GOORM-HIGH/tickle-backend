package com.profect.tickle.domain.notification.controller;

import com.profect.tickle.domain.notification.event.performance.event.PartnerPerformancePublishedEvent;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.performance.dto.response.PerformanceServiceDto;
import com.profect.tickle.global.response.ResultCode;
import com.profect.tickle.global.response.ResultResponse;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/test/notification-event")
@RequiredArgsConstructor
@Slf4j
public class NotificationDevController {

    private final ApplicationEventPublisher eventPublisher;
    private final SseRepository sseRepository;

    @GetMapping("/partner")
    public ResultResponse<Void> publishPartnerPerformanceEvent() {
        log.info("브로드캐스트 요청이 수신되었습니다.");
        // 테스트용 더미 데이터 (Instant는 ISO-8601 full format에 Z 포함)
        PerformanceServiceDto dto = new PerformanceServiceDto(
                1L,
                1L,
                1L,
                1L,
                1L,
                "제목",
                "10000",
                Instant.parse("2025-10-01T19:30:00Z"),
                (short) 120,
                "https://example.com/thumb.jpg",
                Instant.parse("2025-09-10T00:00:00Z"),
                Instant.parse("2025-12-31T00:00:00Z"),
                Boolean.FALSE,
                (short) 0,
                Instant.now(),
                Instant.now(),
                null
        );

        eventPublisher.publishEvent(new PartnerPerformancePublishedEvent(dto));

        log.info("브로드캐스트 요청이 처리되었습니다.");
        return ResultResponse.ok(ResultCode.MEMBER_SIGN_OUT_SUCCES);
    }
}
