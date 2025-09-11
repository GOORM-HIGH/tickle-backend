package com.profect.tickle.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class ChatFileWebSocketMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // WebSocket 관련 메트릭
    private final Counter websocketConnections;
    private final Counter websocketDisconnections;
    private final Counter websocketMessagesSent;
    private final Counter websocketMessagesReceived;
    private final Timer websocketMessageProcessingTime;
    private final ConcurrentMap<String, Integer> activeSessions = new ConcurrentHashMap<>();
    
    // 채팅 관련 메트릭
    private final Counter chatMessagesCreated;
    private final Counter chatMessagesUpdated;
    private final Counter chatMessagesDeleted;
    private final Timer chatMessageRetrievalTime;
    private final Counter unreadMessageCount;
    
    // 파일 관련 메트릭
    private final Counter fileUploads;
    private final Counter fileDownloads;
    private final Timer fileUploadTime;
    private final Timer fileDownloadTime;
    private final Counter s3ApiCalls;
    private final Counter s3ApiErrors;
    
    // JWT 관련 메트릭
    private final Timer jwtValidationTime;
    private final Counter jwtValidationSuccess;
    private final Counter jwtValidationFailure;
    
    public ChatFileWebSocketMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // WebSocket 메트릭 초기화
        this.websocketConnections = Counter.builder("websocket.connections.total")
                .description("Total WebSocket connections")
                .register(meterRegistry);
        
        this.websocketDisconnections = Counter.builder("websocket.disconnections.total")
                .description("Total WebSocket disconnections")
                .register(meterRegistry);
        
        this.websocketMessagesSent = Counter.builder("websocket.messages.sent.total")
                .description("Total WebSocket messages sent")
                .register(meterRegistry);
        
        this.websocketMessagesReceived = Counter.builder("websocket.messages.received.total")
                .description("Total WebSocket messages received")
                .register(meterRegistry);
        
        this.websocketMessageProcessingTime = Timer.builder("websocket.message.processing.time")
                .description("WebSocket message processing time")
                .register(meterRegistry);
        
        // 채팅 메트릭 초기화
        this.chatMessagesCreated = Counter.builder("chat.messages.created.total")
                .description("Total chat messages created")
                .register(meterRegistry);
        
        this.chatMessagesUpdated = Counter.builder("chat.messages.updated.total")
                .description("Total chat messages updated")
                .register(meterRegistry);
        
        this.chatMessagesDeleted = Counter.builder("chat.messages.deleted.total")
                .description("Total chat messages deleted")
                .register(meterRegistry);
        
        this.chatMessageRetrievalTime = Timer.builder("chat.message.retrieval.time")
                .description("Chat message retrieval time")
                .register(meterRegistry);
        
        this.unreadMessageCount = Counter.builder("chat.unread.messages.total")
                .description("Total unread messages")
                .register(meterRegistry);
        
        // 파일 메트릭 초기화
        this.fileUploads = Counter.builder("file.uploads.total")
                .description("Total file uploads")
                .register(meterRegistry);
        
        this.fileDownloads = Counter.builder("file.downloads.total")
                .description("Total file downloads")
                .register(meterRegistry);
        
        this.fileUploadTime = Timer.builder("file.upload.time")
                .description("File upload processing time")
                .register(meterRegistry);
        
        this.fileDownloadTime = Timer.builder("file.download.time")
                .description("File download processing time")
                .register(meterRegistry);
        
        this.s3ApiCalls = Counter.builder("s3.api.calls.total")
                .description("Total S3 API calls")
                .register(meterRegistry);
        
        this.s3ApiErrors = Counter.builder("s3.api.errors.total")
                .description("Total S3 API errors")
                .register(meterRegistry);
        
        // JWT 메트릭 초기화
        this.jwtValidationTime = Timer.builder("jwt.validation.time")
                .description("JWT validation time")
                .register(meterRegistry);
        
        this.jwtValidationSuccess = Counter.builder("jwt.validation.success.total")
                .description("Total successful JWT validations")
                .register(meterRegistry);
        
        this.jwtValidationFailure = Counter.builder("jwt.validation.failure.total")
                .description("Total failed JWT validations")
                .register(meterRegistry);
        
        // 활성 세션 수 Gauge 등록
        Gauge.builder("websocket.active.sessions", this, ChatFileWebSocketMetrics::getActiveSessionCount)
                .description("Active WebSocket sessions")
                .register(meterRegistry);
    }
    
    // WebSocket 메트릭 메서드들
    public void recordWebSocketConnection() {
        websocketConnections.increment();
        log.debug("WebSocket connection recorded");
    }
    
    public void recordWebSocketDisconnection() {
        websocketDisconnections.increment();
        log.debug("WebSocket disconnection recorded");
    }
    
    public void recordWebSocketMessageSent() {
        websocketMessagesSent.increment();
        log.debug("WebSocket message sent recorded");
    }
    
    public void recordWebSocketMessageReceived() {
        websocketMessagesReceived.increment();
        log.debug("WebSocket message received recorded");
    }
    
    public Timer.Sample startWebSocketMessageProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void recordWebSocketMessageProcessingTime(Timer.Sample sample) {
        sample.stop(websocketMessageProcessingTime);
        log.debug("WebSocket message processing time recorded");
    }
    
    public void addActiveSession(String sessionId) {
        activeSessions.put(sessionId, 1);
        log.debug("Active session added: {}", sessionId);
    }
    
    public void removeActiveSession(String sessionId) {
        activeSessions.remove(sessionId);
        log.debug("Active session removed: {}", sessionId);
    }
    
    public double getActiveSessionCount() {
        return activeSessions.size();
    }
    
    // 채팅 메트릭 메서드들
    public void recordChatMessageCreated() {
        chatMessagesCreated.increment();
        log.debug("Chat message created recorded");
    }
    
    public void recordChatMessageUpdated() {
        chatMessagesUpdated.increment();
        log.debug("Chat message updated recorded");
    }
    
    public void recordChatMessageDeleted() {
        chatMessagesDeleted.increment();
        log.debug("Chat message deleted recorded");
    }
    
    public Timer.Sample startChatMessageRetrieval() {
        return Timer.start(meterRegistry);
    }
    
    public void recordChatMessageRetrievalTime(Timer.Sample sample) {
        sample.stop(chatMessageRetrievalTime);
        log.debug("Chat message retrieval time recorded");
    }
    
    public void recordUnreadMessageCount(int count) {
        unreadMessageCount.increment(count);
        log.debug("Unread message count recorded: {}", count);
    }
    
    // 파일 메트릭 메서드들
    public void recordFileUpload() {
        fileUploads.increment();
        log.debug("File upload recorded");
    }
    
    public void recordFileDownload() {
        fileDownloads.increment();
        log.debug("File download recorded");
    }
    
    public Timer.Sample startFileUpload() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileUploadTime(Timer.Sample sample) {
        sample.stop(fileUploadTime);
        log.debug("File upload time recorded");
    }
    
    public Timer.Sample startFileDownload() {
        return Timer.start(meterRegistry);
    }
    
    public void recordFileDownloadTime(Timer.Sample sample) {
        sample.stop(fileDownloadTime);
        log.debug("File download time recorded");
    }
    
    public void recordS3ApiCall() {
        s3ApiCalls.increment();
        log.debug("S3 API call recorded");
    }
    
    public void recordS3ApiError() {
        s3ApiErrors.increment();
        log.debug("S3 API error recorded");
    }
    
    // JWT 메트릭 메서드들
    public Timer.Sample startJwtValidation() {
        return Timer.start(meterRegistry);
    }
    
    public void recordJwtValidationSuccess(Timer.Sample sample) {
        sample.stop(jwtValidationTime);
        jwtValidationSuccess.increment();
        log.debug("JWT validation success recorded");
    }
    
    public void recordJwtValidationFailure(Timer.Sample sample) {
        sample.stop(jwtValidationTime);
        jwtValidationFailure.increment();
        log.debug("JWT validation failure recorded");
    }
}
