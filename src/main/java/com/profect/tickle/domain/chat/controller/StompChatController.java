package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * STOMP 프로토콜 기반 채팅 메시지 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate; // STOMP 메시지 전송용

    /**
     * 채팅방 참여 처리
     * 클라이언트에서 /app/chat.join으로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/chat/{chatRoomId}") // 해당 채팅방 구독자들에게 브로드캐스트
    public WebSocketMessageResponseDto handleJoin(
            @Payload WebSocketMessageRequestDto message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.info("🚪 사용자 채팅방 참여: {} -> 채팅방 {}",
                message.getSenderNickname(), message.getChatRoomId());

        // 세션에 사용자 정보 저장
        headerAccessor.getSessionAttributes().put("username", message.getSenderNickname());
        headerAccessor.getSessionAttributes().put("chatRoomId", message.getChatRoomId());

        return WebSocketMessageResponseDto.builder()
                .type("USER_JOIN")
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                .content(message.getSenderNickname() + "님이 채팅방에 참여했습니다.")
                .createdAt(Instant.now())
                .isMyMessage(false)
                .build();
    }

    /**
     * 채팅 메시지 처리 (안전장치 추가)
     */
    @MessageMapping("/chat.message")
    public void handleMessage(@Payload WebSocketMessageRequestDto message) {
        try {
            log.info("💬 채팅 메시지 수신: {} -> {}",
                    message.getSenderNickname(), message.getContent());

            // 🎯 필수 데이터 검증
            if (message.getChatRoomId() == null) {
                log.error("❌ chatRoomId가 null입니다");
                return;
            }

            if (message.getSenderId() == null) {
                log.error("❌ senderId가 null입니다");
                return;
            }

            log.info("🔍 메시지 전송 요청: chatRoomId={}, senderId={}, type={}",
                    message.getChatRoomId(), message.getSenderId(), message.getMessageType());

            // 🎯 즉시 브로드캐스트 방식으로 변경 (DB 저장 생략하고 테스트)
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("MESSAGE")
                    .messageId((long) (Math.random() * 10000)) // 임시 ID
                    .chatRoomId(message.getChatRoomId())
                    .senderId(message.getSenderId())
                    .senderNickname(message.getSenderNickname())
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .createdAt(Instant.now())
                    .isMyMessage(false) // 모든 사용자에게 일단 false로
                    .build();

            // 🎯 채팅방 전체에 즉시 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatRoomId(),
                    response
            );

            log.info("📤 메시지 브로드캐스트 완료: {}", response);

            // TODO: 나중에 DB 저장 로직 추가
            // var sendRequest = ChatMessageSendRequestDto.builder()...
            // var savedMessage = chatMessageService.sendMessage(...);

        } catch (Exception e) {
            log.error("❌ 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅방 나가기 처리
     * 클라이언트에서 /app/chat.leave로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.leave")
    @SendTo("/topic/chat/{chatRoomId}")
    public WebSocketMessageResponseDto handleLeave(@Payload WebSocketMessageRequestDto message) {
        log.info("🚪 사용자 채팅방 나가기: {} -> 채팅방 {}",
                message.getSenderNickname(), message.getChatRoomId());

        return WebSocketMessageResponseDto.builder()
                .type("USER_LEAVE")
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                .content(message.getSenderNickname() + "님이 채팅방을 나갔습니다.")
                .createdAt(Instant.now())
                .isMyMessage(false)
                .build();
    }

    /**
     * 채팅방 참여자들에게 개별적으로 메시지 전송 (isMyMessage 개별 설정)
     */
    private void sendMessageToAllParticipants(
            WebSocketMessageRequestDto request,
            com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto savedMessage) {

        // TODO: 실제로는 채팅방 참여자 목록을 조회해야 함
        // 현재는 간단한 브로드캐스트로 구현

        WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                .type("MESSAGE")
                .messageId(savedMessage.getId())
                .chatRoomId(request.getChatRoomId())
                .senderId(request.getSenderId())
                .senderNickname(request.getSenderNickname())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .createdAt(savedMessage.getCreatedAt())
                .isMyMessage(false) // 기본값, 실제로는 각 사용자별로 계산 필요
                .build();

        // 🎯 채팅방 전체에 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getChatRoomId(),
                response
        );
    }
}
