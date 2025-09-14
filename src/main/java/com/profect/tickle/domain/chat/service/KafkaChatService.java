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
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 기반 채팅 서비스 (성능 최적화용)
 * 
 * 목표: RabbitMQ 대비 87.5% 처리량 향상
 * - 현재 RabbitMQ: 160,014회/60초 (5,000명)
 * - 목표 Kafka: 300,000회/60초 (5,000명)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaChatService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatParticipantsRepository chatParticipantsRepository;
    private final RedisUnreadCountService redisUnreadCountService;

    // Kafka 토픽 설정
    private static final String CHAT_MESSAGE_TOPIC = "chat-message-topic";
    private static final String CHAT_NOTIFICATION_TOPIC = "chat-notification-topic";
    private static final String CHAT_FILE_TOPIC = "chat-file-topic";

    /**
     * 메시지 전송 (Kafka Producer)
     * 
     * 성능 최적화:
     * - 배치 처리: 16KB 배치 크기
     * - 압축: Snappy 압축
     * - 병렬 처리: 5개 동시 요청
     */
    @Transactional
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, ChatMessageSendRequestDto requestDto) {
        log.info("Kafka 메시지 전송 시작: chatRoomId={}, senderId={}", chatRoomId, senderId);

        try {
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 멤버 존재 여부 확인
            Member member = memberRepository.findById(senderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            // 메시지 타입별 토픽 선택
            String topic = selectTopicByMessageType(requestDto.getMessageType().name());
            
            // Kafka 메시지 전송 (비동기)
            kafkaTemplate.send(topic, chatRoomId.toString(), requestDto)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 메시지 전송 실패: {}", ex.getMessage());
                        } else {
                            log.info("Kafka 메시지 전송 성공: topic={}, partition={}, offset={}", 
                                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });

            // 즉시 응답을 위한 임시 응답 생성
            return ChatMessageResponseDto.builder()
                    .chatRoomId(chatRoomId)
                    .memberId(senderId)
                    .content(requestDto.getContent())
                    .messageType(requestDto.getMessageType())
                    .createdAt(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    .build();

        } catch (Exception e) {
            log.error("Kafka 메시지 전송 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 메시지 처리 (Kafka Consumer)
     * 
     * 성능 최적화:
     * - 배치 처리: 최대 500개 메시지 동시 처리
     * - 병렬 처리: 10개 스레드로 동시 처리
     * - 수동 커밋: 처리 완료 후 커밋
     */
    @KafkaListener(topics = CHAT_MESSAGE_TOPIC, groupId = "chat-group")
    @Transactional
    public void processMessageFromKafka(@Payload ChatMessageSendRequestDto requestDto,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String chatRoomIdStr) {
        
        log.info("Kafka 메시지 처리 시작: topic={}, partition={}, offset={}, chatRoomId={}", 
                topic, partition, offset, chatRoomIdStr);

        try {
            Long chatRoomId = Long.parseLong(chatRoomIdStr);
            
            // 채팅방 존재 여부 확인
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // 메시지 저장
            Chat chat = Chat.builder()
                    .chatRoomId(chatRoomId)
                    .member(memberRepository.findById(1L).orElse(null)) // TODO: 실제 발신자 ID 처리 필요
                    .content(requestDto.getContent())
                    .messageType(requestDto.getMessageType())
                    .isDeleted(false)
                    .build();

            Chat savedMessage = chatRepository.save(chat);

            // WebSocket으로 실시간 전송 (RabbitMQ와 동일한 방식)
            Map<String, Object> webSocketMessage = new HashMap<>();
            webSocketMessage.put("id", savedMessage.getId());
            webSocketMessage.put("chatRoomId", chatRoomId);
            webSocketMessage.put("memberId", savedMessage.getMember().getId());
            webSocketMessage.put("content", requestDto.getContent());
            webSocketMessage.put("messageType", requestDto.getMessageType().name());
            webSocketMessage.put("createdAt", savedMessage.getCreatedAt().toString());

            simpMessagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId,
                    webSocketMessage
            );

            // Redis 캐싱으로 읽지 않은 메시지 수 업데이트
            redisUnreadCountService.incrementUnreadCount(chatRoomId, savedMessage.getMember().getId());

            log.info("Kafka 메시지 처리 완료: chatId={}, chatRoomId={}", savedMessage.getId(), chatRoomId);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 오류 발생: {}", e.getMessage());
            throw e; // 재처리를 위해 예외 발생
        }
    }

    /**
     * 알림 처리 (Kafka Consumer)
     */
    @KafkaListener(topics = CHAT_NOTIFICATION_TOPIC, groupId = "chat-group")
    @Transactional
    public void processNotificationFromKafka(@Payload ChatMessageSendRequestDto requestDto,
                                           @Header(KafkaHeaders.RECEIVED_KEY) String chatRoomIdStr) {
        log.info("Kafka 알림 처리: chatRoomId={}", chatRoomIdStr);
        
        // 알림 처리 로직
        // TODO: 알림 서비스 연동
    }

    /**
     * 파일 처리 (Kafka Consumer)
     */
    @KafkaListener(topics = CHAT_FILE_TOPIC, groupId = "chat-group")
    @Transactional
    public void processFileFromKafka(@Payload ChatMessageSendRequestDto requestDto,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String chatRoomIdStr) {
        log.info("Kafka 파일 처리: chatRoomId={}", chatRoomIdStr);
        
        // 파일 처리 로직
        // TODO: 파일 서비스 연동
    }

    /**
     * 메시지 타입별 토픽 선택
     */
    private String selectTopicByMessageType(String messageType) {
        return switch (messageType.toUpperCase()) {
            case "FILE" -> CHAT_FILE_TOPIC;
            case "NOTIFICATION" -> CHAT_NOTIFICATION_TOPIC;
            default -> CHAT_MESSAGE_TOPIC;
        };
    }

    /**
     * 성능 모니터링용 메트릭
     */
    public void logPerformanceMetrics() {
        // TODO: 메트릭 수집 및 로깅
        log.info("Kafka 성능 메트릭: 처리량, 지연시간, 에러율 등");
    }
}
