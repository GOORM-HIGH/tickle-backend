package com.profect.tickle.domain.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageRequestDto;
import com.profect.tickle.domain.chat.dto.websocket.WebSocketMessageResponseDto;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import com.profect.tickle.domain.chat.service.ChatParticipantsService;
import com.profect.tickle.domain.chat.service.OnlineUserService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.global.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatWebSocketHandler 단위 테스트
 * 
 * 테스트 범위:
 * - WebSocket 연결/종료 처리
 * - 메시지 타입별 라우팅
 * - 채팅방 참여/나가기 처리
 * - 채팅 메시지 브로드캐스팅
 * - 에러 처리
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocket Handler 단위 테스트")
class ChatWebSocketHandlerTest {

    @InjectMocks
    private ChatWebSocketHandler chatWebSocketHandler;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatParticipantsService chatParticipantsService;

    @Mock
    private OnlineUserService onlineUserService;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WebSocketSession webSocketSession;

    // 테스트 데이터
    private final Long CHAT_ROOM_ID = 1L;
    private final Long MEMBER_ID = 100L;
    private final String SESSION_ID = "session123";
    private final String MEMBER_NICKNAME = "testUser";

    // Handler 내부 맵 접근을 위한 필드
    private ConcurrentMap<Long, ConcurrentMap<String, WebSocketSession>> roomSessions;
    private ConcurrentMap<String, Long> sessionToUserId;

    @BeforeEach
    void setUp() {
        // WebSocketSession Mock 설정
        when(webSocketSession.getId()).thenReturn(SESSION_ID);
        when(webSocketSession.isOpen()).thenReturn(true);

        // Handler 내부 맵 초기화
        roomSessions = new ConcurrentHashMap<>();
        sessionToUserId = new ConcurrentHashMap<>();
        
        // ReflectionTestUtils를 사용해서 private 필드에 접근
        ReflectionTestUtils.setField(chatWebSocketHandler, "roomSessions", roomSessions);
        ReflectionTestUtils.setField(chatWebSocketHandler, "sessionToUserId", sessionToUserId);
    }

    @Test
    @DisplayName("TC-WS-001: WebSocket 연결 설정 성공")
    void shouldEstablishConnectionSuccessfully() throws Exception {
        // Given
        URI uri = URI.create("/ws/chat/1");
        when(webSocketSession.getUri()).thenReturn(uri);
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"CONNECTION_SUCCESS\"}");

        // When
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        // Then
        verify(sessionManager).registerSession(
            eq(SESSION_ID), 
            eq(webSocketSession), 
            eq("unknown"), 
            eq("1")
        );
        verify(objectMapper).writeValueAsString(any(WebSocketMessageResponseDto.class));
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-002: 잘못된 채팅방 ID로 연결 시 연결 종료")
    void shouldCloseConnectionWithInvalidChatRoomId() throws Exception {
        // Given
        URI uri = URI.create("/ws/invalid");
        when(webSocketSession.getUri()).thenReturn(uri);

        // When
        chatWebSocketHandler.afterConnectionEstablished(webSocketSession);

        // Then
        verify(webSocketSession).close(any(CloseStatus.class));
        verify(sessionManager, never()).registerSession(anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("TC-WS-003: JOIN 메시지 처리 성공")
    void shouldHandleJoinMessageSuccessfully() throws Exception {
        // Given
        setUpConnectedSession();
        
        WebSocketMessageRequestDto joinRequest = createJoinMessage();
        String jsonRequest = "{\"type\":\"JOIN\",\"chatRoomId\":1,\"senderId\":100}";
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(joinRequest);
        when(onlineUserService.getOnlineCount(CHAT_ROOM_ID)).thenReturn(2);

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(onlineUserService).addOnlineUser(SESSION_ID, CHAT_ROOM_ID, MEMBER_ID);
        verify(sessionManager).registerSession(
            eq(SESSION_ID), 
            eq(webSocketSession), 
            eq(MEMBER_ID.toString()), 
            eq(CHAT_ROOM_ID.toString())
        );
    }

    @Test
    @DisplayName("TC-WS-004: LEAVE 메시지 처리 성공")
    void shouldHandleLeaveMessageSuccessfully() throws Exception {
        // Given
        setUpConnectedSession();
        setUpJoinedUser();
        
        WebSocketMessageRequestDto leaveRequest = createLeaveMessage();
        String jsonRequest = "{\"type\":\"LEAVE\",\"chatRoomId\":1,\"senderId\":100}";
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(leaveRequest);
        when(onlineUserService.getOnlineCount(CHAT_ROOM_ID)).thenReturn(1);

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(onlineUserService).removeOnlineUser(SESSION_ID);
    }

    @Test
    @DisplayName("TC-WS-005: 채팅 메시지 처리 및 브로드캐스트 성공")
    void shouldHandleChatMessageSuccessfully() throws Exception {
        // Given
        setUpConnectedSession();
        setUpJoinedUser();
        
        WebSocketMessageRequestDto chatRequest = createChatMessage();
        String jsonRequest = "{\"type\":\"MESSAGE\",\"chatRoomId\":1,\"senderId\":100}";
        
        ChatMessageResponseDto savedMessage = createChatMessageResponse();
        Member member = createMember();
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(chatRequest);
        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class)))
            .thenReturn(savedMessage);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(onlineUserService.getOnlineCount(CHAT_ROOM_ID)).thenReturn(2);
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"MESSAGE\"}");

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class));
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-006: TYPING 메시지 처리 성공")
    void shouldHandleTypingMessageSuccessfully() throws Exception {
        // Given
        setUpConnectedSession();
        setUpMultipleUsers();
        
        WebSocketMessageRequestDto typingRequest = createTypingMessage();
        String jsonRequest = "{\"type\":\"TYPING\",\"chatRoomId\":1,\"senderId\":100}";
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(typingRequest);
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"TYPING\"}");

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        // 다른 사용자들에게만 타이핑 메시지가 전송되는지 확인
        verify(objectMapper).writeValueAsString(any(WebSocketMessageResponseDto.class));
    }

    @Test
    @DisplayName("TC-WS-007: 지원하지 않는 메시지 타입 처리")
    void shouldHandleUnsupportedMessageType() throws Exception {
        // Given
        setUpConnectedSession();
        
        WebSocketMessageRequestDto invalidRequest = new WebSocketMessageRequestDto();
        invalidRequest.setType("INVALID");
        invalidRequest.setChatRoomId(CHAT_ROOM_ID);
        invalidRequest.setSenderId(MEMBER_ID);
        
        String jsonRequest = "{\"type\":\"INVALID\",\"chatRoomId\":1,\"senderId\":100}";
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(invalidRequest);
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"ERROR\"}");

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-008: JSON 파싱 오류 처리")
    void shouldHandleJsonParsingError() throws Exception {
        // Given
        setUpConnectedSession();
        
        String invalidJson = "{invalid json}";
        when(objectMapper.readValue(invalidJson, WebSocketMessageRequestDto.class))
            .thenThrow(new RuntimeException("JSON parsing error"));
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"ERROR\"}");

        TextMessage textMessage = new TextMessage(invalidJson);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-009: WebSocket 연결 종료 처리")
    void shouldHandleConnectionClosed() throws Exception {
        // Given
        setUpConnectedSession();
        setUpJoinedUser();
        
        CloseStatus closeStatus = CloseStatus.NORMAL;

        // When
        chatWebSocketHandler.afterConnectionClosed(webSocketSession, closeStatus);

        // Then
        verify(onlineUserService).removeOnlineUser(SESSION_ID);
        verify(sessionManager).removeSession(SESSION_ID);
    }

    @Test
    @DisplayName("TC-WS-010: WebSocket 전송 오류 처리")
    void shouldHandleTransportError() throws Exception {
        // Given
        setUpConnectedSession();
        
        Throwable error = new RuntimeException("Transport error");
        when(objectMapper.writeValueAsString(any(WebSocketMessageResponseDto.class)))
            .thenReturn("{\"type\":\"ERROR\"}");

        // When
        chatWebSocketHandler.handleTransportError(webSocketSession, error);

        // Then
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-011: 닫힌 세션에 메시지 전송 시 무시")
    void shouldIgnoreMessageToClosedSession() throws Exception {
        // Given
        setUpConnectedSession();
        setUpJoinedUser();
        when(webSocketSession.isOpen()).thenReturn(false); // 세션이 닫힌 상태
        
        WebSocketMessageRequestDto chatRequest = createChatMessage();
        String jsonRequest = "{\"type\":\"MESSAGE\",\"chatRoomId\":1,\"senderId\":100}";
        
        ChatMessageResponseDto savedMessage = createChatMessageResponse();
        
        when(objectMapper.readValue(jsonRequest, WebSocketMessageRequestDto.class))
            .thenReturn(chatRequest);
        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class)))
            .thenReturn(savedMessage);

        TextMessage textMessage = new TextMessage(jsonRequest);

        // When
        chatWebSocketHandler.handleMessage(webSocketSession, textMessage);

        // Then
        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class));
        // 닫힌 세션에는 메시지가 전송되지 않음
        verify(webSocketSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("TC-WS-012: 부분 메시지 지원 여부 확인")
    void shouldNotSupportPartialMessages() {
        // When
        boolean supportsPartialMessages = chatWebSocketHandler.supportsPartialMessages();
        
        // Then
        assert !supportsPartialMessages;
    }

    // === Helper Methods ===

    private void setUpConnectedSession() {
        ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
        sessions.put(SESSION_ID, webSocketSession);
        roomSessions.put(CHAT_ROOM_ID, sessions);
    }

    private void setUpJoinedUser() {
        sessionToUserId.put(SESSION_ID, MEMBER_ID);
    }

    private void setUpMultipleUsers() {
        // 추가 사용자 세션 생성
        WebSocketSession otherSession = mock(WebSocketSession.class);
        when(otherSession.getId()).thenReturn("session456");
        when(otherSession.isOpen()).thenReturn(true);
        
        ConcurrentMap<String, WebSocketSession> sessions = roomSessions.get(CHAT_ROOM_ID);
        sessions.put("session456", otherSession);
        sessionToUserId.put("session456", 200L);
    }

    private WebSocketMessageRequestDto createJoinMessage() {
        WebSocketMessageRequestDto request = new WebSocketMessageRequestDto();
        request.setType("JOIN");
        request.setChatRoomId(CHAT_ROOM_ID);
        request.setSenderId(MEMBER_ID);
        request.setSenderNickname(MEMBER_NICKNAME);
        return request;
    }

    private WebSocketMessageRequestDto createLeaveMessage() {
        WebSocketMessageRequestDto request = new WebSocketMessageRequestDto();
        request.setType("LEAVE");
        request.setChatRoomId(CHAT_ROOM_ID);
        request.setSenderId(MEMBER_ID);
        request.setSenderNickname(MEMBER_NICKNAME);
        return request;
    }

    private WebSocketMessageRequestDto createChatMessage() {
        WebSocketMessageRequestDto request = new WebSocketMessageRequestDto();
        request.setType("MESSAGE");
        request.setChatRoomId(CHAT_ROOM_ID);
        request.setSenderId(MEMBER_ID);
        request.setSenderNickname(MEMBER_NICKNAME);
        request.setMessageType(ChatMessageType.TEXT);
        request.setContent("안녕하세요!");
        return request;
    }

    private WebSocketMessageRequestDto createTypingMessage() {
        WebSocketMessageRequestDto request = new WebSocketMessageRequestDto();
        request.setType("TYPING");
        request.setChatRoomId(CHAT_ROOM_ID);
        request.setSenderId(MEMBER_ID);
        request.setSenderNickname(MEMBER_NICKNAME);
        return request;
    }

    private ChatMessageResponseDto createChatMessageResponse() {
        return ChatMessageResponseDto.builder()
                .id(1L)
                .memberId(MEMBER_ID)
                .senderNickname(MEMBER_NICKNAME)
                .messageType(ChatMessageType.TEXT)
                .content("안녕하세요!")
                .createdAt(Instant.now())
                .build();
    }

    private Member createMember() {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        ReflectionTestUtils.setField(member, "nickname", MEMBER_NICKNAME);
        return member;
    }
}
