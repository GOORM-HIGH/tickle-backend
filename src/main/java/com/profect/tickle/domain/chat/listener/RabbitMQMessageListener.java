package com.profect.tickle.domain.chat.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMessageListener {

    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 큐 리스너
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(Map<String, Object> messageMap) {
        try {
            log.debug("RabbitMQ에서 채팅 메시지 수신: {}", messageMap);

            Long chatRoomId = Long.valueOf(messageMap.get("chatRoomId").toString());
            Long memberId = Long.valueOf(messageMap.get("memberId").toString());
            String memberName = messageMap.get("memberName").toString();
            String content = messageMap.get("content").toString();
            String messageType = messageMap.get("messageType").toString();

            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + chatRoomId));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

            // WebSocket으로 실시간 전송
            String destination = "/topic/chat/" + chatRoomId;
            String responseMessage = String.format(
                "{\"type\":\"MESSAGE\",\"chatRoomId\":%d,\"memberId\":%d,\"memberName\":\"%s\",\"content\":\"%s\",\"messageType\":\"%s\",\"timestamp\":\"%s\"}",
                chatRoomId, memberId, memberName, content, messageType, System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, responseMessage);
            log.debug("WebSocket으로 채팅 메시지 전송 완료: chatRoomId={}, content={}", chatRoomId, content);

        } catch (Exception e) {
            log.error("채팅 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅 알림 큐 리스너
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_NOTIFICATION_QUEUE)
    public void handleChatNotification(Map<String, Object> notificationMap) {
        try {
            log.debug("RabbitMQ에서 채팅 알림 수신: {}", notificationMap);

            Long recipientId = Long.valueOf(notificationMap.get("recipientId").toString());
            String notificationContent = notificationMap.get("content").toString();

            // 특정 사용자에게 알림 전송
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/notifications",
                    notificationContent
            );
            log.debug("WebSocket으로 알림 메시지 전송 완료: recipientId={}, content={}", recipientId, notificationContent);

        } catch (Exception e) {
            log.error("알림 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅 파일 큐 리스너
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_FILE_QUEUE)
    public void handleChatFile(Map<String, Object> fileMap) {
        try {
            log.debug("RabbitMQ에서 채팅 파일 수신: {}", fileMap);

            Long chatRoomId = Long.valueOf(fileMap.get("chatRoomId").toString());
            Long memberId = Long.valueOf(fileMap.get("memberId").toString());
            String fileName = fileMap.get("fileName").toString();
            String filePath = fileMap.get("filePath").toString();

            // WebSocket으로 파일 메시지 전송
            String destination = "/topic/chat/" + chatRoomId;
            String responseMessage = String.format(
                "{\"type\":\"FILE\",\"chatRoomId\":%d,\"memberId\":%d,\"fileName\":\"%s\",\"filePath\":\"%s\",\"timestamp\":\"%s\"}",
                chatRoomId, memberId, fileName, filePath, System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend(destination, responseMessage);
            log.debug("WebSocket으로 파일 메시지 전송 완료: chatRoomId={}, fileName={}", chatRoomId, fileName);

        } catch (Exception e) {
            log.error("파일 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}