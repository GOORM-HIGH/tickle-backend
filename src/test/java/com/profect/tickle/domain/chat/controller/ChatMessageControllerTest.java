package com.profect.tickle.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageListResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageFileDownloadDto;
import com.profect.tickle.domain.chat.dto.common.PaginationDto;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.service.ChatMessageService;
import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatMessageController 단위 테스트
 * 
 * 테스트 범위:
 * - 메시지 전송 API (텍스트, 파일)
 * - 메시지 목록 조회 API (페이징)
 * - 메시지 수정 API
 * - 메시지 삭제 API
 * - 마지막 메시지 조회 API
 * - 읽지않은 메시지 개수 조회 API
 * - 파일 다운로드 API
 * - HTTP 상태 코드 및 응답 검증
 * - 인증/권한 처리 검증
 */
@WebMvcTest(ChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화
@DisabledInAotMode
@DisplayName("ChatMessage Controller 단위 테스트")
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatMessageService chatMessageService;

    @MockBean
    private FileService fileService;

    @MockBean
    private com.profect.tickle.global.security.util.JwtUtil jwtUtil;

    @MockBean
    private com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;

    @MockBean
    private com.profect.tickle.domain.chat.resolver.CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @MockBean
    private com.profect.tickle.domain.member.repository.MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 테스트 데이터
    private final Long CHAT_ROOM_ID = 1L;
    private final Long MESSAGE_ID = 1L;
    private final Long MEMBER_ID = 100L;
    private final String MEMBER_NICKNAME = "testUser";

    private ChatMessageSendRequestDto textMessageRequest;
    private ChatMessageSendRequestDto fileMessageRequest;
    private ChatMessageResponseDto messageResponse;
    private ChatMessageListResponseDto messageListResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Mock 설정
        when(currentMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(MEMBER_ID);
        
        // ChatJwtAuthenticationInterceptor mock 설정
        when(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // 텍스트 메시지 요청 DTO
        textMessageRequest = ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.TEXT)
                .content("안녕하세요!")
                .build();

        // 파일 메시지 요청 DTO
        fileMessageRequest = ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다")
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileSize(1024)
                .fileType("text/plain")
                .build();

        // 메시지 응답 DTO
        messageResponse = ChatMessageResponseDto.builder()
                .id(MESSAGE_ID)
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(MEMBER_ID)
                .senderNickname(MEMBER_NICKNAME)
                .messageType(ChatMessageType.TEXT)
                .content("안녕하세요!")
                .createdAt(Instant.now())
                .isMyMessage(true)
                .build();

        // 메시지 목록 응답 DTO
        PaginationDto pagination = PaginationDto.of(0, 50, 1L);
        messageListResponse = ChatMessageListResponseDto.builder()
                .messages(Arrays.asList(messageResponse))
                .pagination(pagination)
                .build();
    }

    // ===== 메시지 전송 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-001: 텍스트 메시지 전송 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldSendTextMessageSuccessfully() throws Exception {
        // Given
        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class)))
                .thenReturn(messageResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(textMessageRequest)))
                .andDo(print()) // 응답 내용 출력
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("생성 성공"))
                .andExpect(jsonPath("$.data.id").value(MESSAGE_ID))
                .andExpect(jsonPath("$.data.content").value("안녕하세요!"))
                .andExpect(jsonPath("$.data.messageType").value("TEXT"))
                .andExpect(jsonPath("$.data.senderNickname").value(MEMBER_NICKNAME));

        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-002: 파일 메시지 전송 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldSendFileMessageSuccessfully() throws Exception {
        // Given
        ChatMessageResponseDto fileMessageResponse = ChatMessageResponseDto.builder()
                .id(MESSAGE_ID)
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(MEMBER_ID)
                .senderNickname(MEMBER_NICKNAME)
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다")
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileSize(1024)
                .fileType("text/plain")
                .createdAt(Instant.now())
                .isMyMessage(true)
                .build();

        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class)))
                .thenReturn(fileMessageResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fileMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.messageType").value("FILE"))
                .andExpect(jsonPath("$.data.fileName").value("test-file.txt"))
                .andExpect(jsonPath("$.data.fileSize").value(1024));

        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-003: 빈 내용으로 메시지 전송 실패")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldFailToSendEmptyMessage() throws Exception {
        // Given
        ChatMessageSendRequestDto emptyRequest = ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.TEXT)
                .content("")
                .build();

        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class)))
                .thenThrow(new BusinessException(ErrorCode.CHAT_MESSAGE_EMPTY_CONTENT));

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatMessageSendRequestDto.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-006: 채팅방에 참여하지 않은 사용자가 메시지 전송 실패")
    @WithMockMember(id = 999, email = "non-participant@example.com")
    void shouldFailToSendMessageByNonParticipant() throws Exception {
        // Given
        when(chatMessageService.sendMessage(eq(CHAT_ROOM_ID), eq(999L), any(ChatMessageSendRequestDto.class)))
                .thenThrow(new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(textMessageRequest)))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).sendMessage(eq(CHAT_ROOM_ID), eq(999L), any(ChatMessageSendRequestDto.class));
    }

    // ===== 메시지 목록 조회 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-007: 메시지 목록 조회 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldGetMessageListSuccessfully() throws Exception {
        // Given
        when(chatMessageService.getMessages(eq(CHAT_ROOM_ID), eq(MEMBER_ID), eq(0), eq(50), any()))
                .thenReturn(messageListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.data.pagination.currentPage").value(0));

        verify(chatMessageService).getMessages(eq(CHAT_ROOM_ID), eq(MEMBER_ID), eq(0), eq(50), any());
    }

    @Test
    @DisplayName("TC-MESSAGE-008: 음수 페이지 값으로 메시지 목록 조회 실패")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldFailToGetMessageListWithNegativePage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages", CHAT_ROOM_ID)
                .param("page", "-1")
                .param("size", "50"))
                .andExpect(status().isInternalServerError());
    }

    // ===== 메시지 수정 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-009: 메시지 수정 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldEditMessageSuccessfully() throws Exception {
        // Given
        String newContent = "수정된 메시지 내용";
        ChatMessageResponseDto editedResponse = ChatMessageResponseDto.builder()
                .id(MESSAGE_ID)
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(MEMBER_ID)
                .senderNickname(MEMBER_NICKNAME)
                .messageType(ChatMessageType.TEXT)
                .content(newContent)
                .createdAt(Instant.now())
                .isMyMessage(true)
                .build();

        when(chatMessageService.editMessage(eq(MESSAGE_ID), eq(MEMBER_ID), eq(newContent)))
                .thenReturn(editedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}", CHAT_ROOM_ID, MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"" + newContent + "\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value(newContent));

        verify(chatMessageService).editMessage(eq(MESSAGE_ID), eq(MEMBER_ID), eq(newContent));
    }

    @Test
    @DisplayName("TC-MESSAGE-010: 다른 사용자의 메시지 수정 실패")
    @WithMockMember(id = 999, email = "other@example.com")
    void shouldFailToEditOtherUserMessage() throws Exception {
        // Given
        String newContent = "수정 시도";
        when(chatMessageService.editMessage(eq(MESSAGE_ID), eq(999L), eq(newContent)))
                .thenThrow(new BusinessException(ErrorCode.CHAT_NOT_MESSAGE_OWNER));

        // When & Then
        mockMvc.perform(put("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}", CHAT_ROOM_ID, MESSAGE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"" + newContent + "\""))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).editMessage(eq(MESSAGE_ID), eq(999L), eq(newContent));
    }

    // ===== 메시지 삭제 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-012: 메시지 삭제 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldDeleteMessageSuccessfully() throws Exception {
        // Given
        doNothing().when(chatMessageService).deleteMessage(eq(MESSAGE_ID), eq(MEMBER_ID));

        // When & Then
        mockMvc.perform(delete("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."));

        verify(chatMessageService).deleteMessage(eq(MESSAGE_ID), eq(MEMBER_ID));
    }

    // ===== 마지막 메시지 조회 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-014: 마지막 메시지 조회 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldGetLastMessageSuccessfully() throws Exception {
        // Given
        when(chatMessageService.getLastMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID)))
                .thenReturn(messageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/last", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(MESSAGE_ID))
                .andExpect(jsonPath("$.data.content").value("안녕하세요!"));

        verify(chatMessageService).getLastMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-MESSAGE-015: 메시지가 없는 채팅방의 마지막 메시지 조회")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldGetLastMessageWhenNoMessages() throws Exception {
        // Given
        when(chatMessageService.getLastMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID)))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/last", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("메시지가 없습니다."));

        verify(chatMessageService).getLastMessage(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    // ===== 읽지않은 메시지 개수 조회 테스트 =====

    @Test
    @DisplayName("TC-READ-003: 읽지않은 메시지 개수 조회 성공")
    @WithMockMember(id = 100, email = "test@example.com")
    void shouldGetUnreadCountSuccessfully() throws Exception {
        // Given
        int unreadCount = 3;
        when(chatMessageService.getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any()))
                .thenReturn(unreadCount);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/unread-count", CHAT_ROOM_ID)
                .param("lastReadMessageId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(unreadCount));

        verify(chatMessageService).getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID), eq(5L));
    }

    // ===== 파일 다운로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-001: 파일 다운로드 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldDownloadFileSuccessfully() throws Exception {
        // Given
        ChatMessageFileDownloadDto fileInfo = ChatMessageFileDownloadDto.builder()
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileType("text/plain")
                .fileSize(1024)
                .build();

        ByteArrayResource resource = new ByteArrayResource("test file content".getBytes());
        
        when(chatMessageService.getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID)))
                .thenReturn(fileInfo);
        when(fileService.downloadFile(anyString(), anyString()))
                .thenReturn(resource);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}/download", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''test-file.txt"))
                .andExpect(header().string("Content-Type", "text/plain"));

        verify(chatMessageService).getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID));
        verify(fileService).downloadFile(eq("/uploads/chat/test-file.txt"), eq("test-file.txt"));
    }

    @Test
    @DisplayName("TC-FILE-002: 파일 다운로드 실패 - 권한 없음")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenDownloadingFileWithoutPermission() throws Exception {
        // Given
        when(chatMessageService.getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID)))
                .thenThrow(new RuntimeException("채팅방 참여 권한이 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}/download", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID));
        verify(fileService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("TC-FILE-003: 파일 다운로드 실패 - 파일이 첨부되지 않은 메시지")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenDownloadingNonFileMessage() throws Exception {
        // Given
        when(chatMessageService.getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID)))
                .thenThrow(new IllegalArgumentException("파일이 첨부되지 않은 메시지입니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}/download", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID));
        verify(fileService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("TC-FILE-004: 파일 다운로드 실패 - 파일 경로 없음")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenDownloadingFileWithNoPath() throws Exception {
        // Given
        when(chatMessageService.getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID)))
                .thenThrow(new IllegalArgumentException("파일 경로가 존재하지 않습니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}/download", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID));
        verify(fileService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("TC-FILE-005: 파일 다운로드 실패 - 파일 서비스 오류")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenFileServiceThrowsException() throws Exception {
        // Given
        ChatMessageFileDownloadDto fileInfo = ChatMessageFileDownloadDto.builder()
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileType("text/plain")
                .fileSize(1024)
                .build();

        when(chatMessageService.getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID)))
                .thenReturn(fileInfo);
        when(fileService.downloadFile(anyString(), anyString()))
                .thenThrow(new RuntimeException("파일을 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/messages/{messageId}/download", CHAT_ROOM_ID, MESSAGE_ID))
                .andExpect(status().isInternalServerError());

        verify(chatMessageService).getMessageFileForDownload(eq(CHAT_ROOM_ID), eq(MESSAGE_ID), eq(MEMBER_ID));
        verify(fileService).downloadFile(eq("/uploads/chat/test-file.txt"), eq("test-file.txt"));
    }
}
