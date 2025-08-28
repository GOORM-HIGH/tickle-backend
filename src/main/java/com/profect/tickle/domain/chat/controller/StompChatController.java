package com.profect.tickle.domain.chat.controller;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * STOMP 프로토콜 기반 채팅 메시지 컨트롤러
 * JWT 검증은 StompJwtChannelInterceptor에서 처리됨
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberRepository memberRepository;

    /**
     * 채팅방 참여 처리
     * 클라이언트에서 /app/chat.join으로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.join")
    public void handleJoin(
            @Payload WebSocketMessageRequestDto message,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            // 인터셉터에서 검증된 사용자 정보 추출
            Long userId = getUserIdFromHeader(headerAccessor);
            Member member = getMemberById(userId);
            
            log.info("사용자 채팅방 참여: memberId={}, nickname={}, chatRoomId={}",
                    userId, member.getNickname(), message.getChatRoomId());

            // 세션에 사용자 정보 저장
            headerAccessor.getSessionAttributes().put("userId", userId);
            headerAccessor.getSessionAttributes().put("username", member.getNickname());
            headerAccessor.getSessionAttributes().put("chatRoomId", message.getChatRoomId());

            // 참여 메시지 생성
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("USER_JOIN")
                    .chatRoomId(message.getChatRoomId())
                    .senderId(userId)
                    .senderNickname(member.getNickname())
                    .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                    .content(member.getNickname() + "님이 채팅방에 참여했습니다.")
                    .createdAt(Instant.now())
                    .build();

            // 채팅방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatRoomId(),
                    response
            );

        } catch (Exception e) {
            log.error("채팅방 참여 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅 메시지 처리 (DB 저장 포함)
     */
    @MessageMapping("/chat.message")
    public void handleMessage(
            @Payload WebSocketMessageRequestDto message, 
            SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            // 인터셉터에서 검증된 사용자 정보 추출
            Long userId = getUserIdFromHeader(headerAccessor);
            Member member = getMemberById(userId);
            
            log.info("채팅 메시지 수신: memberId={}, nickname={}, chatRoomId={}, content={}",
                    userId, member.getNickname(), message.getChatRoomId(), message.getContent());

            // 필수 데이터 검증
            validateMessage(message);

            // DB에 메시지 저장
            ChatMessageSendRequestDto sendRequest = ChatMessageSendRequestDto.builder()
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .build();

            var savedMessage = chatMessageService.sendMessage(
                    message.getChatRoomId(), 
                    userId,
                    sendRequest
            );

            log.info("메시지 DB 저장 완료: messageId={}", savedMessage.getId());

            // 응답 메시지 생성
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("MESSAGE")
                    .messageId(savedMessage.getId())
                    .chatRoomId(message.getChatRoomId())
                    .senderId(userId)
                    .senderNickname(member.getNickname())
                    .messageType(message.getMessageType())
                    .content(message.getContent())
                    .createdAt(savedMessage.getCreatedAt())
                    .build();

            // 채팅방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatRoomId(),
                    response
            );

            log.info("메시지 브로드캐스트 완료: messageId={}", savedMessage.getId());

        } catch (Exception e) {
            log.error("메시지 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅방 나가기 처리
     * 클라이언트에서 /app/chat.leave로 메시지 전송 시 호출
     */
    @MessageMapping("/chat.leave")
    public void handleLeave(
            @Payload WebSocketMessageRequestDto message, 
            SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            // 인터셉터에서 검증된 사용자 정보 추출
            Long userId = getUserIdFromHeader(headerAccessor);
            Member member = getMemberById(userId);
            
            log.info("사용자 채팅방 나가기: memberId={}, nickname={}, chatRoomId={}",
                    userId, member.getNickname(), message.getChatRoomId());

            // 나가기 메시지 생성
            WebSocketMessageResponseDto response = WebSocketMessageResponseDto.builder()
                    .type("USER_LEAVE")
                    .chatRoomId(message.getChatRoomId())
                    .senderId(userId)
                    .senderNickname(member.getNickname())
                    .messageType(com.profect.tickle.domain.chat.entity.ChatMessageType.SYSTEM)
                    .content(member.getNickname() + "님이 채팅방을 나갔습니다.")
                    .createdAt(Instant.now())
                    .build();

            // 채팅방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatRoomId(),
                    response
            );

        } catch (Exception e) {
            log.error("채팅방 나가기 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 헤더에서 사용자 ID 추출 (인터셉터에서 검증된 정보)
     */
    private Long getUserIdFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        // 인터셉터에서 설정한 헤더에서 userId 추출
        Object userIdObj = headerAccessor.getHeader("userId");
        if (userIdObj == null) {
            log.error("헤더에서 사용자 ID를 찾을 수 없습니다");
            throw new BusinessException(ErrorCode.CHAT_PERMISSION_DENIED);
        }
        return (Long) userIdObj;
    }

    /**
     * 사용자 ID로 Member 엔티티 조회
     */
    private Member getMemberById(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 메시지 데이터 검증
     */
    private void validateMessage(WebSocketMessageRequestDto message) {
        if (message.getChatRoomId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (message.getMessageType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
