package com.profect.tickle.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatRoomJoinRequestDto;
import com.profect.tickle.domain.chat.dto.request.ReadMessageRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import com.profect.tickle.domain.chat.service.ChatParticipantsService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatParticipantsController 단위 테스트
 * 
 * 테스트 범위:
 * - 채팅방 참여 API
 * - 채팅방 나가기 API
 * - 메시지 읽음 처리 API
 * - 읽지않은 메시지 개수 조회 API
 * - 채팅방 참여자 목록 조회 API
 * - 내 채팅방 목록 조회 API
 * - HTTP 상태 코드 및 응답 검증
 * - 인증/권한 처리 검증
 */
@WebMvcTest(ChatParticipantsController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화
@DisabledInAotMode
@DisplayName("ChatParticipants Controller 단위 테스트")
class ChatParticipantsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatParticipantsService chatParticipantsService;

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
    private final Long MEMBER_ID = 6L;
    private final Long MESSAGE_ID = 100L;

    private ChatRoomJoinRequestDto joinRequestDto;
    private ReadMessageRequestDto readRequestDto;
    private ChatParticipantsResponseDto participantsResponseDto;
    private UnreadCountResponseDto unreadCountResponseDto;

    @BeforeEach
    void setUp() throws Exception {
        // 요청 DTO 설정
        joinRequestDto = new ChatRoomJoinRequestDto("안녕하세요!");
        readRequestDto = new ReadMessageRequestDto(MESSAGE_ID);

        // 응답 DTO 설정
        participantsResponseDto = ChatParticipantsResponseDto.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .memberId(MEMBER_ID)
                .memberNickname("테스트사용자")
                .joinedAt(Instant.now())
                .lastReadMessageId(MESSAGE_ID)
                .unreadMessageCount(5)
                .isOnline(true)
                .build();

        unreadCountResponseDto = UnreadCountResponseDto.builder()
                .unreadCount(5)
                .lastReadMessageId(MESSAGE_ID)
                .lastReadAt(Instant.now())
                .build();

        // CurrentMemberArgumentResolver Mock 설정
        when(currentMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(MEMBER_ID);
        
        // ChatJwtAuthenticationInterceptor mock 설정
        when(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-001: 채팅방 참여 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldJoinChatRoomSuccessfully() throws Exception {
        // Given
        when(chatParticipantsService.joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class)))
            .thenReturn(participantsResponseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/participants/rooms/{chatRoomId}/join", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.chatRoomId").value(CHAT_ROOM_ID))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.memberNickname").value("테스트사용자"))
                .andExpect(jsonPath("$.data.unreadMessageCount").value(5));

        verify(chatParticipantsService).joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-002: 채팅방 정원 초과로 참여 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToJoinChatRoomWhenFull() throws Exception {
        // Given
        when(chatParticipantsService.joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_CAPACITY_EXCEEDED));

        // When & Then
        mockMvc.perform(post("/api/v1/chat/participants/rooms/{chatRoomId}/join", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequestDto)))
                .andExpect(status().isBadRequest());

        verify(chatParticipantsService).joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-003: 존재하지 않는 채팅방 참여 시도 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToJoinNonExistentChatRoom() throws Exception {
        // Given
        Long nonExistentChatRoomId = 999L;
        when(chatParticipantsService.joinChatRoom(eq(nonExistentChatRoomId), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/chat/participants/rooms/{chatRoomId}/join", nonExistentChatRoomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinRequestDto)))
                .andExpect(status().isNotFound());

        verify(chatParticipantsService).joinChatRoom(eq(nonExistentChatRoomId), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-004: 채팅방 나가기 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldLeaveChatRoomSuccessfully() throws Exception {
        // Given
        doNothing().when(chatParticipantsService).leaveChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID));

        // When & Then
        mockMvc.perform(delete("/api/v1/chat/participants/rooms/{chatRoomId}/leave", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("채팅방 나가기 성공"));

        verify(chatParticipantsService).leaveChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-005: 참여하지 않은 채팅방 나가기 시도 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToLeaveNonParticipatedChatRoom() throws Exception {
        // Given
        doThrow(new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND))
            .when(chatParticipantsService).leaveChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID));

        // When & Then
        mockMvc.perform(delete("/api/v1/chat/participants/rooms/{chatRoomId}/leave", CHAT_ROOM_ID))
                .andExpect(status().isNotFound());

        verify(chatParticipantsService).leaveChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-006: 메시지 읽음 처리 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldMarkAsReadSuccessfully() throws Exception {
        // Given
        doNothing().when(chatParticipantsService).markAsRead(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ReadMessageRequestDto.class));

        // When & Then
        mockMvc.perform(patch("/api/v1/chat/participants/rooms/{chatRoomId}/read", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(readRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("메시지 읽음 처리 성공"));

        verify(chatParticipantsService).markAsRead(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ReadMessageRequestDto.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-007: 읽지않은 메시지 개수 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetUnreadCountSuccessfully() throws Exception {
        // Given
        when(chatParticipantsService.getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID)))
            .thenReturn(unreadCountResponseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/participants/rooms/{chatRoomId}/unread-count", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.unreadCount").value(5))
                .andExpect(jsonPath("$.data.lastReadMessageId").value(MESSAGE_ID));

        verify(chatParticipantsService).getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-008: 채팅방 참여자 목록 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetParticipantsSuccessfully() throws Exception {
        // Given
        List<ChatParticipantsResponseDto> participantsList = List.of(participantsResponseDto);
        when(chatParticipantsService.getParticipantsByRoomId(eq(CHAT_ROOM_ID)))
            .thenReturn(participantsList);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/participants/rooms/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].chatRoomId").value(CHAT_ROOM_ID))
                .andExpect(jsonPath("$.data[0].memberId").value(MEMBER_ID));

        verify(chatParticipantsService).getParticipantsByRoomId(eq(CHAT_ROOM_ID));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-009: 내 채팅방 목록 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetMyChatRoomsSuccessfully() throws Exception {
        // Given
        List<ChatParticipantsResponseDto> myRoomsList = List.of(participantsResponseDto);
        when(chatParticipantsService.getMyChatRooms(eq(MEMBER_ID)))
            .thenReturn(myRoomsList);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/participants/my-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].chatRoomId").value(CHAT_ROOM_ID))
                .andExpect(jsonPath("$.data[0].memberId").value(MEMBER_ID));

        verify(chatParticipantsService).getMyChatRooms(eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-010: 빈 메시지로 채팅방 참여 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldJoinChatRoomWithEmptyMessageSuccessfully() throws Exception {
        // Given - 빈 메시지로 참여 (message는 선택사항)
        ChatRoomJoinRequestDto emptyMessageRequest = new ChatRoomJoinRequestDto("");
        when(chatParticipantsService.joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class)))
            .thenReturn(participantsResponseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/participants/rooms/{chatRoomId}/join", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyMessageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201));

        verify(chatParticipantsService).joinChatRoom(eq(CHAT_ROOM_ID), eq(MEMBER_ID), any(ChatRoomJoinRequestDto.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-011: 유효하지 않은 요청으로 메시지 읽음 처리 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToMarkAsReadWithInvalidRequest() throws Exception {
        // Given - 필수 필드 누락
        ReadMessageRequestDto invalidRequest = new ReadMessageRequestDto(null);

        // When & Then
        mockMvc.perform(patch("/api/v1/chat/participants/rooms/{chatRoomId}/read", CHAT_ROOM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(chatParticipantsService, never()).markAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-012: 존재하지 않는 채팅방의 참여자 목록 조회 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToGetParticipantsForNonExistentChatRoom() throws Exception {
        // Given
        Long nonExistentChatRoomId = 999L;
        when(chatParticipantsService.getParticipantsByRoomId(eq(nonExistentChatRoomId)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/participants/rooms/{chatRoomId}", nonExistentChatRoomId))
                .andExpect(status().isNotFound());

        verify(chatParticipantsService).getParticipantsByRoomId(eq(nonExistentChatRoomId));
    }

    @Test
    @DisplayName("TC-PARTICIPANTS-013: 참여하지 않은 채팅방의 읽지않은 메시지 개수 조회 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToGetUnreadCountForNonParticipatedChatRoom() throws Exception {
        // Given
        when(chatParticipantsService.getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/participants/rooms/{chatRoomId}/unread-count", CHAT_ROOM_ID))
                .andExpect(status().isNotFound());

        verify(chatParticipantsService).getUnreadCount(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }
}
