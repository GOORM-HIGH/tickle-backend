package com.profect.tickle.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQChatService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 채팅 메시지를 RabbitMQ로 전송
     */
    public void sendMessage(String message) {
        try {
            log.debug("RabbitMQ로 메시지 전송: {}", message);
            
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            
            // 채팅 큐로 메시지 전송
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                messageMap
            );
            
            log.debug("RabbitMQ 메시지 전송 완료");
            
        } catch (Exception e) {
            log.error("RabbitMQ 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 알림 메시지를 RabbitMQ로 전송
     */
    public void sendNotification(String notification) {
        try {
            log.debug("RabbitMQ로 알림 전송: {}", notification);
            
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> notificationMap = objectMapper.readValue(notification, Map.class);
            
            // 알림 큐로 메시지 전송
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                notificationMap
            );
            
            log.debug("RabbitMQ 알림 전송 완료");
            
        } catch (Exception e) {
            log.error("RabbitMQ 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 파일 메시지를 RabbitMQ로 전송
     */
    public void sendFileMessage(String fileMessage) {
        try {
            log.debug("RabbitMQ로 파일 메시지 전송: {}", fileMessage);
            
            // JSON 문자열을 Map으로 파싱
            Map<String, Object> fileMap = objectMapper.readValue(fileMessage, Map.class);
            
            // 파일 큐로 메시지 전송
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.FILE_ROUTING_KEY,
                fileMap
            );
            
            log.debug("RabbitMQ 파일 메시지 전송 완료");
            
        } catch (Exception e) {
            log.error("RabbitMQ 파일 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }
}