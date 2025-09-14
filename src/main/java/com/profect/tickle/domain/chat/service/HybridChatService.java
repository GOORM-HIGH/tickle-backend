package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 하이브리드 채팅 서비스 (RabbitMQ + Kafka)
 * 
 * 목표: 점진적 마이그레이션을 통한 성능 최적화
 * - Phase 1: 10% Kafka, 90% RabbitMQ
 * - Phase 2: 50% Kafka, 50% RabbitMQ  
 * - Phase 3: 100% Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridChatService {

    private final RabbitMQChatService rabbitMQChatService;
    private final KafkaChatService kafkaChatService;

    @Value("${chat.migration.kafka-ratio:0.1}")
    private double kafkaRatio;

    @Value("${chat.migration.enabled:true}")
    private boolean migrationEnabled;

    /**
     * 하이브리드 메시지 전송
     * 
     * 성능 최적화 전략:
     * - 중요 메시지: Kafka 처리 (성능 우선)
     * - 일반 메시지: RabbitMQ 처리 (안정성 우선)
     * - 점진적 마이그레이션: 설정으로 비율 조절
     */
    public ChatMessageResponseDto sendMessage(Long chatRoomId, Long senderId, ChatMessageSendRequestDto requestDto) {
        log.info("하이브리드 메시지 전송 시작: chatRoomId={}, senderId={}, kafkaRatio={}", 
                chatRoomId, senderId, kafkaRatio);

        try {
            // 마이그레이션 비활성화 시 RabbitMQ만 사용
            if (!migrationEnabled) {
                log.info("마이그레이션 비활성화: RabbitMQ 사용");
                return rabbitMQChatService.sendMessage(chatRoomId, senderId, requestDto);
            }

            // 메시지 타입별 처리 방식 결정
            boolean useKafka = shouldUseKafka(requestDto, senderId);
            
            if (useKafka) {
                log.info("Kafka 처리 선택: chatRoomId={}", chatRoomId);
                return kafkaChatService.sendMessage(chatRoomId, senderId, requestDto);
            } else {
                log.info("RabbitMQ 처리 선택: chatRoomId={}", chatRoomId);
                return rabbitMQChatService.sendMessage(chatRoomId, senderId, requestDto);
            }

        } catch (Exception e) {
            log.error("하이브리드 메시지 전송 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Kafka 사용 여부 결정
     * 
     * 결정 기준:
     * 1. 설정된 비율 (kafkaRatio)
     * 2. 메시지 타입 (중요도)
     * 3. 채팅방 ID (일관성)
     */
    private boolean shouldUseKafka(ChatMessageSendRequestDto requestDto, Long senderId) {
        // 1. 설정된 비율에 따른 랜덤 선택
        boolean randomSelection = Math.random() < kafkaRatio;
        
        // 2. 메시지 타입별 우선순위
        boolean importantMessage = isImportantMessage(requestDto);
        
        // 3. 채팅방 ID 기반 일관성 (같은 채팅방은 같은 방식 사용)
        boolean consistentSelection = (senderId % 2 == 0);
        
        // 최종 결정: 중요 메시지이거나 랜덤 선택이거나 일관성 선택
        boolean useKafka = importantMessage || randomSelection || consistentSelection;
        
        log.debug("Kafka 사용 결정: important={}, random={}, consistent={}, result={}", 
                importantMessage, randomSelection, consistentSelection, useKafka);
        
        return useKafka;
    }

    /**
     * 중요 메시지 판단
     */
    private boolean isImportantMessage(ChatMessageSendRequestDto requestDto) {
        // 중요 메시지 기준
        return "FILE".equals(requestDto.getMessageType().name()) || 
               "NOTIFICATION".equals(requestDto.getMessageType().name()) ||
               requestDto.getContent().length() > 100; // 긴 메시지
    }

    /**
     * 마이그레이션 비율 업데이트
     */
    public void updateMigrationRatio(double newRatio) {
        if (newRatio < 0.0 || newRatio > 1.0) {
            throw new IllegalArgumentException("마이그레이션 비율은 0.0 ~ 1.0 사이여야 합니다: " + newRatio);
        }
        
        this.kafkaRatio = newRatio;
        log.info("마이그레이션 비율 업데이트: {}% Kafka, {}% RabbitMQ", 
                (int)(newRatio * 100), (int)((1 - newRatio) * 100));
    }

    /**
     * 마이그레이션 상태 조회
     */
    public MigrationStatus getMigrationStatus() {
        return MigrationStatus.builder()
                .kafkaRatio(kafkaRatio)
                .migrationEnabled(migrationEnabled)
                .build();
    }

    /**
     * 마이그레이션 상태 클래스
     */
    public static class MigrationStatus {
        private final double kafkaRatio;
        private final double rabbitMQRatio;
        private final boolean migrationEnabled;

        public MigrationStatus(double kafkaRatio, double rabbitMQRatio, boolean migrationEnabled) {
            this.kafkaRatio = kafkaRatio;
            this.rabbitMQRatio = rabbitMQRatio;
            this.migrationEnabled = migrationEnabled;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private double kafkaRatio;
            private double rabbitMQRatio;
            private boolean migrationEnabled;

            public Builder kafkaRatio(double kafkaRatio) {
                this.kafkaRatio = kafkaRatio;
                this.rabbitMQRatio = 1.0 - kafkaRatio;
                return this;
            }

            public Builder migrationEnabled(boolean migrationEnabled) {
                this.migrationEnabled = migrationEnabled;
                return this;
            }

            public MigrationStatus build() {
                return new MigrationStatus(kafkaRatio, rabbitMQRatio, migrationEnabled);
            }
        }

        // Getters
        public double getKafkaRatio() { return kafkaRatio; }
        public double getRabbitMQRatio() { return rabbitMQRatio; }
        public boolean isMigrationEnabled() { return migrationEnabled; }
    }
}
