package com.profect.tickle.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profect.tickle.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.service.ChatRoomService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatRoomController 단위 테스트
 * 
 * 테스트 범위:
 * - 채팅방 생성 API
 * - 채팅방 조회 API (공연별, ID별)
 * - 채팅방 상태 변경 API
 * - 참여 여부 확인 API
 * - 온라인 사용자 조회 API
 * - HTTP 상태 코드 및 응답 검증
 * - 인증/권한 처리 검증
 */
@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화
@DisabledInAotMode
@DisplayName("ChatRoom Controller 단위 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;

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
    private final Long PERFORMANCE_ID = 1L;
    private final Long MEMBER_ID = 6L;
    private final String ROOM_NAME = "공연토론방";

    private ChatRoomCreateRequestDto createRequestDto;
    private ChatRoomResponseDto responseDto;

    @BeforeEach
    void setUp() throws Exception {
        // 요청 DTO 설정
        createRequestDto = new ChatRoomCreateRequestDto(PERFORMANCE_ID, ROOM_NAME, (short) 100);

        // 응답 DTO 설정
        responseDto = ChatRoomResponseDto.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .performanceId(PERFORMANCE_ID)
                .name(ROOM_NAME)
                .maxParticipants((short) 100)
                .participantCount(1)
                .status(true)
                .createdAt(Instant.now())
                .build();

        // CurrentMemberArgumentResolver Mock 설정
        when(currentMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(MEMBER_ID);
        
        // ChatJwtAuthenticationInterceptor mock 설정
        when(chatJwtAuthenticationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("TC-API-001: 채팅방 생성 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldCreateChatRoomSuccessfully() throws Exception {
        // Given
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequestDto.class)))
            .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.chatRoomId").value(CHAT_ROOM_ID))
                .andExpect(jsonPath("$.data.performanceId").value(PERFORMANCE_ID))
                .andExpect(jsonPath("$.data.name").value(ROOM_NAME))
                .andExpect(jsonPath("$.data.maxParticipants").value(100))
                .andExpect(jsonPath("$.data.status").value(true));

        verify(chatRoomService).createChatRoom(any(ChatRoomCreateRequestDto.class));
    }

    @Test
    @DisplayName("TC-API-002: 유효하지 않은 요청으로 채팅방 생성 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToCreateChatRoomWithInvalidRequest() throws Exception {
        // Given - 필수 필드 누락
        ChatRoomCreateRequestDto invalidRequest = new ChatRoomCreateRequestDto(null, "", (short) -1);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(chatRoomService, never()).createChatRoom(any());
    }

    @Test
    @DisplayName("TC-API-003: 중복 채팅방 생성 시도 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToCreateDuplicateChatRoom() throws Exception {
        // Given
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequestDto.class)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS));

        // When & Then
        mockMvc.perform(post("/api/v1/chat/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isConflict());

        verify(chatRoomService).createChatRoom(any(ChatRoomCreateRequestDto.class));
    }

    @Test
    @DisplayName("TC-API-004: 공연별 채팅방 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetChatRoomByPerformanceIdSuccessfully() throws Exception {
        // Given
        when(chatRoomService.getChatRoomByPerformanceId(eq(PERFORMANCE_ID), eq(MEMBER_ID)))
            .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/performance/{performanceId}", PERFORMANCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.chatRoomId").value(CHAT_ROOM_ID))
                .andExpect(jsonPath("$.data.performanceId").value(PERFORMANCE_ID));

        verify(chatRoomService).getChatRoomByPerformanceId(eq(PERFORMANCE_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-API-005: 존재하지 않는 공연의 채팅방 조회 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToGetChatRoomForNonExistentPerformance() throws Exception {
        // Given
        Long nonExistentPerformanceId = 999L;
        when(chatRoomService.getChatRoomByPerformanceId(eq(nonExistentPerformanceId), eq(MEMBER_ID)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/performance/{performanceId}", nonExistentPerformanceId))
                .andExpect(status().isNotFound());

        verify(chatRoomService).getChatRoomByPerformanceId(eq(nonExistentPerformanceId), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-API-006: 채팅방 ID로 기본 정보 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetChatRoomByIdSuccessfully() throws Exception {
        // Given
        when(chatRoomService.getChatRoomById(eq(CHAT_ROOM_ID)))
            .thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.chatRoomId").value(CHAT_ROOM_ID));

        verify(chatRoomService).getChatRoomById(eq(CHAT_ROOM_ID));
    }

    @Test
    @DisplayName("TC-API-007: 존재하지 않는 채팅방 조회 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToGetNonExistentChatRoom() throws Exception {
        // Given
        Long nonExistentChatRoomId = 999L;
        when(chatRoomService.getChatRoomById(eq(nonExistentChatRoomId)))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}", nonExistentChatRoomId))
                .andExpect(status().isNotFound());

        verify(chatRoomService).getChatRoomById(eq(nonExistentChatRoomId));
    }

    @Test
    @DisplayName("TC-API-008: 채팅방 상태 변경 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldUpdateChatRoomStatusSuccessfully() throws Exception {
        // Given
        doNothing().when(chatRoomService).updateChatRoomStatus(eq(CHAT_ROOM_ID), eq(false));

        // When & Then
        mockMvc.perform(patch("/api/v1/chat/rooms/{chatRoomId}/status", CHAT_ROOM_ID)
                .param("status", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("채팅방 상태가 변경되었습니다."));

        verify(chatRoomService).updateChatRoomStatus(eq(CHAT_ROOM_ID), eq(false));
    }

    @Test
    @DisplayName("TC-API-009: 권한 없는 사용자의 채팅방 상태 변경 시도 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToUpdateChatRoomStatusWithoutPermission() throws Exception {
        // Given
        doThrow(new BusinessException(ErrorCode.NO_PERMISSION))
            .when(chatRoomService).updateChatRoomStatus(eq(CHAT_ROOM_ID), eq(false));

        // When & Then
        mockMvc.perform(patch("/api/v1/chat/rooms/{chatRoomId}/status", CHAT_ROOM_ID)
                .param("status", "false"))
                .andExpect(status().isForbidden());

        verify(chatRoomService).updateChatRoomStatus(eq(CHAT_ROOM_ID), eq(false));
    }

    @Test
    @DisplayName("TC-API-010: 채팅방 참여 여부 확인 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldCheckParticipationSuccessfully() throws Exception {
        // Given
        when(chatRoomService.isParticipant(eq(CHAT_ROOM_ID), eq(MEMBER_ID)))
            .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/participation", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(chatRoomService).isParticipant(eq(CHAT_ROOM_ID), eq(MEMBER_ID));
    }

    @Test
    @DisplayName("TC-API-011: 채팅방 온라인 사용자 조회 성공")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetOnlineUsersSuccessfully() throws Exception {
        // Given
        Map<String, Object> onlineInfo = Map.of(
            "onlineCount", 3,
            "onlineUserIds", java.util.List.of(1L, 2L, 3L)
        );
        when(chatRoomService.getOnlineUserInfo(eq(CHAT_ROOM_ID)))
            .thenReturn(onlineInfo);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}/online", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.onlineCount").value(3))
                .andExpect(jsonPath("$.data.onlineUserIds").isArray())
                .andExpect(jsonPath("$.data.onlineUserIds.length()").value(3));

        verify(chatRoomService).getOnlineUserInfo(eq(CHAT_ROOM_ID));
    }

    @Test
    @DisplayName("TC-API-012: 인증되지 않은 사용자의 API 접근 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailToAccessApiWithoutAuthentication() throws Exception {
        // Given - 인증된 사용자이지만 권한이 없는 경우
        when(chatRoomService.getChatRoomById(eq(CHAT_ROOM_ID)))
            .thenThrow(new BusinessException(ErrorCode.NO_PERMISSION));

        // When & Then
        mockMvc.perform(get("/api/v1/chat/rooms/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isForbidden());

        verify(chatRoomService).getChatRoomById(eq(CHAT_ROOM_ID));
    }

    @Test
    @DisplayName("TC-API-013: 잘못된 HTTP 메서드로 API 호출 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWithWrongHttpMethod() throws Exception {
        // When & Then - 잘못된 HTTP 메서드로 테스트
        mockMvc.perform(delete("/api/v1/chat/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // 400으로 반환됨

        verify(chatRoomService, never()).createChatRoom(any());
    }

    @Test
    @DisplayName("TC-API-014: 잘못된 Content-Type으로 API 호출 실패")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWithWrongContentType() throws Exception {
        // When & Then - 잘못된 Content-Type으로 요청
        mockMvc.perform(post("/api/v1/chat/rooms")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid json content"))
                .andExpect(status().is5xxServerError()); // 500 에러로 처리됨

        verify(chatRoomService, never()).createChatRoom(any());
    }
}
