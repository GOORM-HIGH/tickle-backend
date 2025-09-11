package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatMessageMapper;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.domain.chat.repository.ChatRepository;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.config.RabbitMQConfig;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 기반 채팅 서비스 (메시지 동시 전송 처리)
 * 
 * 주요 기능:
 * 1. 메시지 동시 전송 처리 (순서 보장)
 * 2. 메시지 손실 방지
 * 3. 시스템 과부하 시 메시지 대기열 관리
 * 4. 비동기 처리로 응답 속도 향상
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQChatService {

    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatParticipantsRepository chatParticipantsRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final RedisUnreadCountService redisUnreadCountService;

    /**
     * 메시지 전송 (RabbitMQ 큐에 전송)
     * 
     * @param chatRoomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param requestDto 메시지 전송 요청
     * @return 메시지 응답 DTO
     */
    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, ChatMessageSendRequestDto requestDto) {
        try {
            // 1. 발신자 및 채팅방 유효성 검사
            Member sender = memberRepository.findById(senderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 2. 메시지를 RabbitMQ 큐에 전송
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("chatRoomId", chatRoomId);
            messageData.put("senderId", senderId);
            messageData.put("content", requestDto.getContent());
            messageData.put("messageType", requestDto.getMessageType().toString());
            messageData.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    RabbitMQConfig.MESSAGE_ROUTING_KEY,
                    messageData
            );

            log.info("메시지가 RabbitMQ 큐에 전송됨: chatRoomId={}, senderId={}, content={}", 
                    chatRoomId, senderId, requestDto.getContent());

            // 3. 즉시 응답 반환 (사용자 경험 향상)
            return ChatMessageResponseDto.builder()
                    .id(0L) // 임시 ID (실제 ID는 큐 처리 후 생성)
                    .chatRoomId(chatRoomId)
                    .memberId(senderId)
                    .content(requestDto.getContent())
                    .messageType(requestDto.getMessageType())
                    .createdAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생: chatRoomId={}, senderId={}, error={}", 
                    chatRoomId, senderId, e.getMessage(), e);
            throw new BusinessException("메시지 전송 중 오류 발생", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * RabbitMQ 큐에서 메시지 처리 (비동기)
     * 
     * @param messageData 메시지 데이터
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    @Transactional
    public void processMessage(Map<String, Object> messageData) {
        try {
            Long chatRoomId = Long.valueOf(messageData.get("chatRoomId").toString());
            Long senderId = Long.valueOf(messageData.get("senderId").toString());
            String content = messageData.get("content").toString();
            String messageType = messageData.get("messageType").toString();
            LocalDateTime timestamp = LocalDateTime.parse(messageData.get("timestamp").toString());

            // 1. 메시지 저장
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
            
            Member sender = memberRepository.findById(senderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Chat message = Chat.builder()
                    .chatRoomId(chatRoom.getId())
                    .member(sender)
                    .content(content)
                    .messageType(ChatMessageType.valueOf(messageType))
                    .createdAt(timestamp.toInstant(ZoneOffset.UTC))
                    .isDeleted(false)
                    .build();

            Chat savedMessage = chatRepository.save(message);

            // 2. WebSocket으로 실시간 메시지 전송
            Map<String, Object> webSocketMessage = new HashMap<>();
            webSocketMessage.put("id", savedMessage.getId());
            webSocketMessage.put("chatRoomId", chatRoomId);
            webSocketMessage.put("memberId", senderId);
            webSocketMessage.put("content", content);
            webSocketMessage.put("messageType", messageType);
            webSocketMessage.put("createdAt", timestamp.toString());

            simpMessagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId,
                    webSocketMessage
            );

            // 3. Redis 캐시 업데이트 (읽지 않은 메시지 개수)
            redisUnreadCountService.incrementUnreadCount(chatRoomId, senderId);

            // 4. 알림 큐에 전송
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("chatRoomId", chatRoomId);
            notificationData.put("messageId", savedMessage.getId());
            notificationData.put("senderId", senderId);
            notificationData.put("content", content);
            notificationData.put("timestamp", timestamp.toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    notificationData
            );

            log.info("메시지 처리 완료: messageId={}, chatRoomId={}, senderId={}", 
                    savedMessage.getId(), chatRoomId, senderId);

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: messageData={}, error={}", 
                    messageData, e.getMessage(), e);
            // 메시지 처리 실패 시 재시도 로직 추가 가능
        }
    }

    /**
     * 알림 처리 (비동기)
     * 
     * @param notificationData 알림 데이터
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_NOTIFICATION_QUEUE)
    @Transactional
    public void processNotification(Map<String, Object> notificationData) {
        try {
            Long chatRoomId = Long.valueOf(notificationData.get("chatRoomId").toString());
            Long messageId = Long.valueOf(notificationData.get("messageId").toString());
            Long senderId = Long.valueOf(notificationData.get("senderId").toString());
            String content = notificationData.get("content").toString();

            // 읽지 않은 메시지 개수 업데이트
            // 실제 구현에서는 Redis나 별도 테이블에서 관리
            log.debug("읽지 않은 메시지 개수 업데이트: chatRoomId={}, messageId={}, senderId={}", 
                    chatRoomId, messageId, senderId);

            log.info("알림 처리 완료: chatRoomId={}, messageId={}, senderId={}", 
                    chatRoomId, messageId, senderId);

        } catch (Exception e) {
            log.error("알림 처리 중 오류 발생: notificationData={}, error={}", 
                    notificationData, e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 메시지 개수 조회 (Redis 캐싱 최적화)
     * 
     * @param chatRoomId 채팅방 ID
     * @param memberId 회원 ID
     * @return 읽지 않은 메시지 개수
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(Long chatRoomId, Long memberId) {
        try {
            // Redis 캐싱을 통한 최적화된 조회
            int unreadCount = redisUnreadCountService.getUnreadCount(chatRoomId, memberId);

            log.info("읽지않은 메시지 개수 조회 결과 (Redis 최적화): chatRoomId={}, memberId={}, unreadCount={}",
                    chatRoomId, memberId, unreadCount);

            return unreadCount;

        } catch (Exception e) {
            log.error("읽지않은 메시지 개수 조회 중 오류 발생: chatRoomId={}, memberId={}, error={}",
                    chatRoomId, memberId, e.getMessage(), e);
            throw new BusinessException("읽지 않은 메시지 개수 조회 중 오류 발생", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
