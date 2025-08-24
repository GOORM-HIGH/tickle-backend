package com.profect.tickle.global.websocket;

import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.global.response.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket 정보", description = "WebSocket 연결 상태 및 정보 조회 API")
public class WebSocketInfoController {

    private final WebSocketSessionManager sessionManager;

    @Value("${server.port:8081}")
    private String serverPort;

    /**
     * WebSocket 연결 정보 조회
     */
    @Operation(
            summary = "WebSocket 연결 정보 조회",
            description = "WebSocket 연결 URL과 엔드포인트 정보를 조회합니다."
    )
    @GetMapping("/connection-info")
    public ResultResponse<Map<String, Object>> getConnectionInfo(
            @RequestParam(required = false, defaultValue = "localhost") String host) {

        Map<String, Object> connectionInfo = new HashMap<>();

        // WebSocket 연결 URL들
        connectionInfo.put("chatWebSocketUrl", String.format("ws://%s:%s/ws/chat", host, serverPort));
        connectionInfo.put("chatSockJSUrl", String.format("http://%s:%s/ws/chat", host, serverPort));

        // 연결 가이드
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("chat", "/ws/chat/{chatRoomId}");
        connectionInfo.put("endpoints", endpoints);

        // 사용 예시 (프론트엔드 개발자용)
        Map<String, String> examples = new HashMap<>();
        examples.put("chatConnection", String.format("new WebSocket('ws://%s:%s/ws/chat/123')", host, serverPort));
        examples.put("sockJSConnection", String.format("new SockJS('http://%s:%s/ws/chat/123')", host, serverPort));
        connectionInfo.put("examples", examples);

        return ResultResponse.of(ResultCode.RESPONSE_TEST, connectionInfo);
    }

    /**
     * WebSocket 연결 상태 조회
     */
    @Operation(
            summary = "WebSocket 연결 상태 조회",
            description = "현재 WebSocket 연결 상태와 세션 정보를 조회합니다."
    )
    @GetMapping("/status")
    public ResultResponse<WebSocketStats> getWebSocketStatus() {

        WebSocketStats stats = sessionManager.getStats();

        return ResultResponse.of(ResultCode.RESPONSE_TEST, stats);
    }
}
