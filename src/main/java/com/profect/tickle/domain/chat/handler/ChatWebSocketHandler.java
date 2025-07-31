package com.profect.tickle.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import com.profect.tickle.domain.chat.service.ChatParticipantsService;
import com.profect.tickle.domain.chat.service.OnlineUserService;
import com.profect.tickle.global.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ChatParticipantsService chatParticipantsService;
    private final OnlineUserService onlineUserService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // 채팅 전용 세션 관리 (roomId -> sessionId -> WebSocketSession)
    private final ConcurrentMap<Long, ConcurrentMap<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("채팅 WebSocket 연결 설정: sessionId={}", session.getId());

        // URL에서 chatRoomId 추출
        Long chatRoomId = extractChatRoomId(session);
        if (chatRoomId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("잘못된 채팅방 ID"));
            return;
        }

        // 채팅방별 세션 관리
        roomSessions.computeIfAbsent(chatRoomId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        // Global 세션 관리자에도 등록
        sessionManager.registerSession(session.getId(), session, "unknown", chatRoomId.toString());

        log.info("채팅방 {} 세션 추가 완료: sessionId={}", chatRoomId, session.getId());

        // 연결 성공 메시지 전송
        sendConnectionSuccessMessage(session, chatRoomId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            log.debug("WebSocket 메시지 수신: sessionId={}, payload={}", session.getId(), payload);

            try {
                WebSocketMessageRequestDto requestDto = objectMapper.readValue(payload, WebSocketMessageRequestDto.class);

                // 메시지 타입별 처리
                handleWebSocketMessage(session, requestDto);

            } catch (Exception e) {
                log.error("메시지 처리 중 오류 발생: sessionId={}, error={}", session.getId(), e.getMessage(), e);
                sendErrorMessage(session, "메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("채팅 WebSocket 전송 오류: sessionId={}, error={}", session.getId(), exception.getMessage());

        try {
            sendErrorMessage(session, "연결 오류가 발생했습니다. 다시 연결해주세요.");
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("채팅 WebSocket 연결 종료: sessionId={}, status={}", session.getId(), closeStatus);

        // 온라인 사용자에서 제거
        onlineUserService.removeOnlineUser(session.getId());

        // 전역 세션 관리자에서 제거
        sessionManager.removeSession(session.getId());

        // 채팅방별 세션에서 제거
        roomSessions.values().forEach(sessions -> sessions.remove(session.getId()));

        log.info("채팅 세션 정리 완료: sessionId={}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 연결 성공 메시지 전송
     */
    private void sendConnectionSuccessMessage(WebSocketSession session, Long chatRoomId) {
        try {
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("CONNECTION_SUCCESS")
                    .chatRoomId(chatRoomId)
                    .message("채팅방 연결이 완료되었습니다. JOIN 메시지를 전송해주세요.")
                    .build();

            String messageJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(messageJson));
        } catch (Exception e) {
            log.error("연결 성공 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * URL에서 chatRoomId 추출
     */
    private Long extractChatRoomId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath();
                String[] segments = path.split("/");
                for (int i = 0; i < segments.length; i++) {
                    if ("chat".equals(segments[i]) && i + 1 < segments.length) {
                        return Long.parseLong(segments[i + 1]);
                    }
                }
            }
        } catch (Exception e) {
            log.error("chatRoomId 추출 실패: sessionId={}, uri={}, error={}",
                    session.getId(), session.getUri(), e.getMessage());
        }
        return null;
    }

    /**
     * WebSocket 메시지 타입별 처리
     */
    private void handleWebSocketMessage(WebSocketSession session, WebSocketMessageRequestDto requestDto) throws Exception {
        switch (requestDto.getType().toUpperCase()) {
            case "JOIN":
                handleJoinMessage(session, requestDto);
                break;
            case "MESSAGE":
                handleChatMessage(session, requestDto);
                break;
            case "LEAVE":
                handleLeaveMessage(session, requestDto);
                break;
            case "TYPING":
                handleTypingMessage(session, requestDto);
                break;
            default:
                sendErrorMessage(session, "지원하지 않는 메시지 타입입니다: " + requestDto.getType());
        }
    }

    /**
     * 채팅방 참여 처리
     */
    private void handleJoinMessage(WebSocketSession session, WebSocketMessageRequestDto requestDto) throws Exception {
        Long chatRoomId = requestDto.getChatRoomId();
        Long memberId = requestDto.getSenderId();

        try {
            // 온라인 사용자 추가
            onlineUserService.addOnlineUser(session.getId(), chatRoomId, memberId);

            // Global 세션 관리자 업데이트
            sessionManager.registerSession(session.getId(), session, memberId.toString(), chatRoomId.toString());

            // 다른 사용자들에게 참여 알림
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("USER_JOIN")
                    .chatRoomId(chatRoomId)
                    .senderId(memberId)
                    .senderNickname(requestDto.getSenderNickname())
                    .onlineCount(onlineUserService.getOnlineCount(chatRoomId))
                    .message(requestDto.getSenderNickname() + "님이 채팅방에 참여했습니다")
                    .build();

            broadcastToRoom(chatRoomId, response);
            log.info("사용자 채팅방 참여: chatRoomId={}, memberId={}", chatRoomId, memberId);

        } catch (Exception e) {
            log.error("채팅방 참여 처리 오류: {}", e.getMessage(), e);
            sendErrorMessage(session, "채팅방 참여에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅 메시지 처리
     */
    private void handleChatMessage(WebSocketSession session, WebSocketMessageRequestDto requestDto) throws Exception {
        try {
            // REST API용 DTO로 변환 (생성자 방식 대신 Builder 패턴 사용)
            ChatMessageSendRequestDto sendRequest = ChatMessageSendRequestDto.builder()
                    .messageType(requestDto.getMessageType())
                    .content(requestDto.getContent())
                    .filePath(requestDto.getFilePath())
                    .fileName(requestDto.getFileName())
                    .fileSize(requestDto.getFileSize())
                    .fileType(requestDto.getFileType())
                    .build();

            // ChatMessageService를 통해 메시지 저장
            ChatMessageResponseDto savedMessage = chatMessageService.sendMessage(
                    requestDto.getChatRoomId(),
                    requestDto.getSenderId(),
                    sendRequest
            );

            // WebSocket 응답 DTO로 변환
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("MESSAGE")
                    .messageId(savedMessage.getId())
                    .chatRoomId(requestDto.getChatRoomId())
                    .senderId(requestDto.getSenderId())
                    .senderNickname(requestDto.getSenderNickname())
                    .messageType(requestDto.getMessageType())
                    .content(requestDto.getContent())
                    .filePath(requestDto.getFilePath())
                    .fileName(requestDto.getFileName())
                    .fileSize(requestDto.getFileSize())
                    .fileType(requestDto.getFileType())
                    .createdAt(savedMessage.getCreatedAt())
                    .onlineCount(onlineUserService.getOnlineCount(requestDto.getChatRoomId()))
                    .build();

            // 채팅방의 모든 사용자에게 브로드캐스트
            broadcastToRoom(requestDto.getChatRoomId(), response);
            log.info("채팅 메시지 브로드캐스트 완료: messageId={}", savedMessage.getId());

        } catch (Exception e) {
            log.error("채팅 메시지 처리 오류: {}", e.getMessage(), e);
            sendErrorMessage(session, "메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅방 나가기 처리
     */
    private void handleLeaveMessage(WebSocketSession session, WebSocketMessageRequestDto requestDto) throws Exception {
        Long chatRoomId = requestDto.getChatRoomId();
        Long memberId = requestDto.getSenderId();

        try {
            // 온라인 사용자에서 제거
            onlineUserService.removeOnlineUser(session.getId());

            // 다른 사용자들에게 나가기 알림
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("USER_LEAVE")
                    .chatRoomId(chatRoomId)
                    .senderId(memberId)
                    .senderNickname(requestDto.getSenderNickname())
                    .onlineCount(onlineUserService.getOnlineCount(chatRoomId))
                    .message(requestDto.getSenderNickname() + "님이 채팅방을 나갔습니다")
                    .build();

            broadcastToRoom(chatRoomId, response);
            log.info("사용자 채팅방 나가기: chatRoomId={}, memberId={}", chatRoomId, memberId);

        } catch (Exception e) {
            log.error("채팅방 나가기 처리 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 타이핑 상태 처리
     */
    private void handleTypingMessage(WebSocketSession session, WebSocketMessageRequestDto requestDto) throws Exception {
        try {
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("TYPING")
                    .chatRoomId(requestDto.getChatRoomId())
                    .senderId(requestDto.getSenderId())
                    .senderNickname(requestDto.getSenderNickname())
                    .message(requestDto.getSenderNickname() + "님이 입력 중입니다...")
                    .build();

            // 본인 제외하고 브로드캐스트
            broadcastToRoomExcept(requestDto.getChatRoomId(), response, session.getId());

        } catch (Exception e) {
            log.error("타이핑 메시지 처리 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 에러 메시지 전송
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("ERROR")
                    .message(errorMessage)
                    .build();

            String messageJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(messageJson));
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 채팅방의 모든 사용자에게 메시지 브로드캐스트
     */
    private void broadcastToRoom(Long chatRoomId, WebSocketMessageResponseDto message) {
        ConcurrentMap<String, WebSocketSession> sessions = roomSessions.get(chatRoomId);
        if (sessions != null) {
            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("메시지 JSON 변환 오류: {}", e.getMessage());
                return;
            }

            sessions.values().parallelStream().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(messageJson));
                    }
                } catch (IOException e) {
                    log.error("메시지 전송 실패: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            });
        }
    }

    /**
     * 특정 세션 제외하고 브로드캐스트
     */
    private void broadcastToRoomExcept(Long chatRoomId, WebSocketMessageResponseDto message, String excludeSessionId) {
        ConcurrentMap<String, WebSocketSession> sessions = roomSessions.get(chatRoomId);
        if (sessions != null) {
            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (Exception e) {
                log.error("메시지 JSON 변환 오류: {}", e.getMessage());
                return;
            }

            sessions.entrySet().parallelStream()
                    .filter(entry -> !entry.getKey().equals(excludeSessionId))
                    .forEach(entry -> {
                        try {
                            WebSocketSession session = entry.getValue();
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(messageJson));
                            }
                        } catch (IOException e) {
                            log.error("메시지 전송 실패: sessionId={}, error={}", entry.getKey(), e.getMessage());
                        }
                    });
        }
    }
}
