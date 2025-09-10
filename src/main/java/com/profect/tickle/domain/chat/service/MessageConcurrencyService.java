package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 메시지 전송 동시성 처리 서비스
 * 
 * 성능 최적화 포인트:
 * 1. 메시지 배치 처리로 DB 부하 감소
 * 2. 동시성 제어로 데이터 일관성 보장
 * 3. 비동기 처리로 응답 속도 향상
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConcurrencyService {

    private final ChatRepository chatRepository;
    private final UnreadCountOptimizer unreadCountOptimizer;
    
    // 메시지 배치 처리를 위한 큐
    private final List<Chat> messageBatch = new ArrayList<>();
    private final ReentrantLock batchLock = new ReentrantLock();
    
    // 동시성 제어를 위한 세마포어
    private final Semaphore messageProcessingSemaphore = new Semaphore(10); // 최대 10개 동시 처리
    
    // 메시지 전송 순서 보장을 위한 락
    private final Object messageOrderLock = new Object();
    
    // 배치 처리 설정
    private static final int BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MS = 100; // 100ms

    /**
     * 메시지 전송 (동시성 제어)
     * 
     * @param message 전송할 메시지
     * @return CompletableFuture<Void>
     */
    @Async("messageTaskExecutor")
    public CompletableFuture<Void> sendMessage(Chat message) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 세마포어로 동시 처리 수 제한
                messageProcessingSemaphore.acquire();
                
                synchronized (messageOrderLock) {
                    // 메시지 순서 보장
                    processMessageWithOrder(message);
                }
                
            } catch (InterruptedException e) {
                log.error("메시지 처리 중 인터럽트 발생: messageId={}, error={}", 
                        message.getId(), e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("메시지 전송 실패: messageId={}, error={}", 
                        message.getId(), e.getMessage(), e);
            } finally {
                messageProcessingSemaphore.release();
            }
        });
    }

    /**
     * 메시지 순서 보장 처리
     * 
     * @param message 처리할 메시지
     */
    private void processMessageWithOrder(Chat message) {
        try {
            // 1. 메시지를 배치 큐에 추가
            addToBatch(message);
            
            // 2. 읽지 않은 메시지 개수 증가 (Redis)
            unreadCountOptimizer.incrementUnreadCount(
                message.getChatRoomId(), 
                message.getMember().getId(), 
                1
            );
            
            log.debug("메시지 순서 보장 처리 완료: messageId={}, roomId={}, memberId={}", 
                    message.getId(), message.getChatRoomId(), message.getMember().getId());
                    
        } catch (Exception e) {
            log.error("메시지 순서 보장 처리 실패: messageId={}, error={}", 
                    message.getId(), e.getMessage(), e);
        }
    }

    /**
     * 메시지를 배치 큐에 추가
     * 
     * @param message 추가할 메시지
     */
    private void addToBatch(Chat message) {
        batchLock.lock();
        try {
            messageBatch.add(message);
            
            // 배치 크기에 도달하면 즉시 처리
            if (messageBatch.size() >= BATCH_SIZE) {
                processBatch();
            }
            
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 배치 처리 실행
     */
    private void processBatch() {
        List<Chat> batchToProcess;
        
        batchLock.lock();
        try {
            if (messageBatch.isEmpty()) {
                return;
            }
            
            // 배치 복사 및 초기화
            batchToProcess = new ArrayList<>(messageBatch);
            messageBatch.clear();
            
        } finally {
            batchLock.unlock();
        }
        
        // 배치로 DB 저장
        try {
            chatRepository.saveAll(batchToProcess);
            
            log.info("메시지 배치 처리 완료: size={}", batchToProcess.size());
            
        } catch (Exception e) {
            log.error("메시지 배치 처리 실패: size={}, error={}", 
                    batchToProcess.size(), e.getMessage(), e);
            
            // 실패한 메시지들을 다시 큐에 추가
            batchLock.lock();
            try {
                messageBatch.addAll(0, batchToProcess); // 앞쪽에 추가
            } finally {
                batchLock.unlock();
            }
        }
    }

    /**
     * 주기적 배치 처리 (100ms마다)
     */
    @Scheduled(fixedDelay = BATCH_DELAY_MS)
    public void scheduledBatchProcess() {
        if (!messageBatch.isEmpty()) {
            processBatch();
        }
    }

    /**
     * 강제 배치 처리 (애플리케이션 종료 시 등)
     */
    public void forceBatchProcess() {
        batchLock.lock();
        try {
            if (!messageBatch.isEmpty()) {
                processBatch();
            }
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 현재 배치 큐 크기 조회
     * 
     * @return 배치 큐 크기
     */
    public int getBatchQueueSize() {
        batchLock.lock();
        try {
            return messageBatch.size();
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * 메시지 전송 통계 조회
     * 
     * @return 통계 정보
     */
    public MessageStats getMessageStats() {
        return MessageStats.builder()
                .batchQueueSize(getBatchQueueSize())
                .availablePermits(messageProcessingSemaphore.availablePermits())
                .totalPermits(messageProcessingSemaphore.availablePermits() + 
                             (10 - messageProcessingSemaphore.availablePermits()))
                .build();
    }

    /**
     * 메시지 전송 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class MessageStats {
        private int batchQueueSize;
        private int availablePermits;
        private int totalPermits;
    }
}
