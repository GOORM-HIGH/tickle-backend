package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatRoomResponseDto;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatRoomMapper;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.performance.entity.Performance;
import com.profect.tickle.domain.performance.repository.PerformanceRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.profect.tickle.testsecurity.WithMockMember;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 단위 테스트")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private PerformanceRepository performanceRepository;
    
    @Mock
    private ChatRoomMapper chatRoomMapper;
    
    @Mock
    private OnlineUserService onlineUserService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    // ===== 채팅방 생성 테스트 =====

    @Test
    @DisplayName("TC-CHATROOM-001: 유효한 공연 정보로 채팅방을 생성한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldCreateChatRoomSuccessfully() {
        // Given
        ChatRoomCreateRequestDto requestDto = createChatRoomCreateRequest();
        Performance performance = createTestPerformance(1L);
        ChatRoom savedRoom = createTestChatRoom(1L, 1L, "공연토론방", true);
        
        given(performanceRepository.findById(1L)).willReturn(Optional.of(performance));
        given(chatRoomRepository.findByPerformanceId(1L)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
        
        // When
        ChatRoomResponseDto result = chatRoomService.createChatRoom(requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRoomId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("공연토론방");
        assertThat(result.getPerformanceId()).isEqualTo(1L);
        assertThat(result.getStatus()).isTrue();
        assertThat(result.getMaxParticipants()).isEqualTo((short) 50);
        
        verify(performanceRepository).findById(1L);
        verify(chatRoomRepository).findByPerformanceId(1L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("TC-CHATROOM-002: 존재하지 않는 공연으로 채팅방 생성을 시도한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenCreatingChatRoomWithNonExistentPerformance() {
        // Given
        ChatRoomCreateRequestDto requestDto = createChatRoomCreateRequest();
        
        given(performanceRepository.findById(1L)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.createChatRoom(requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.performanceNotFoundInChat()
        
        verify(performanceRepository).findById(1L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("TC-CHATROOM-003: 이미 채팅방이 있는 공연에 중복 생성을 시도한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenCreatingDuplicateChatRoom() {
        // Given
        ChatRoomCreateRequestDto requestDto = createChatRoomCreateRequest();
        Performance performance = createTestPerformance(1L);
        ChatRoom existingRoom = createTestChatRoom(1L, 1L, "기존 채팅방", true);
        
        given(performanceRepository.findById(1L)).willReturn(Optional.of(performance));
        given(chatRoomRepository.findByPerformanceId(1L)).willReturn(Optional.of(existingRoom));
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.createChatRoom(requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomAlreadyExists()
        
        verify(performanceRepository).findById(1L);
        verify(chatRoomRepository).findByPerformanceId(1L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    // ===== 채팅방 조회 테스트 =====

    @Test
    @DisplayName("TC-CHATROOM-004: 공연 ID로 채팅방을 조회한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetChatRoomByPerformanceIdSuccessfully() {
        // Given
        Long performanceId = 1L;
        Long currentMemberId = 1L;
        ChatRoomResponseDto expectedResponse = createChatRoomResponseDto();
        
        given(chatRoomMapper.findRoomDetailsByPerformanceId(performanceId, currentMemberId))
                .willReturn(expectedResponse);
        
        // When
        ChatRoomResponseDto result = chatRoomService.getChatRoomByPerformanceId(performanceId, currentMemberId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRoomId()).isEqualTo(1L);
        assertThat(result.getPerformanceId()).isEqualTo(performanceId);
        
        verify(chatRoomMapper).findRoomDetailsByPerformanceId(performanceId, currentMemberId);
    }

    @Test
    @DisplayName("TC-CHATROOM-005: 존재하지 않는 공연의 채팅방을 조회한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenGettingNonExistentChatRoom() {
        // Given
        Long performanceId = 999L;
        Long currentMemberId = 1L;
        
        given(chatRoomMapper.findRoomDetailsByPerformanceId(performanceId, currentMemberId))
                .willReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.getChatRoomByPerformanceId(performanceId, currentMemberId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomNotFoundByPerformance()
        
        verify(chatRoomMapper).findRoomDetailsByPerformanceId(performanceId, currentMemberId);
    }

    @Test
    @DisplayName("TC-CHATROOM-006: 유효한 채팅방 ID로 조회한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldGetChatRoomByIdSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, 1L, "테스트 채팅방", true);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        
        // When
        ChatRoomResponseDto result = chatRoomService.getChatRoomById(chatRoomId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(result.getName()).isEqualTo("테스트 채팅방");
        
        verify(chatRoomRepository).findById(chatRoomId);
    }

    @Test
    @DisplayName("TC-CHATROOM-007: 존재하지 않는 채팅방 ID로 조회한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldFailWhenGettingNonExistentChatRoomById() {
        // Given
        Long chatRoomId = 999L;
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.getChatRoomById(chatRoomId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomNotFound()
        
        verify(chatRoomRepository).findById(chatRoomId);
    }

    // ===== 채팅방 상태 관리 테스트 =====

    @Test
    @DisplayName("TC-CHATROOM-008: 유효한 채팅방의 상태를 변경한다")
    @WithMockMember(id = 6, email = "ahn3931@naver.com", roles = {"HOST"})
    void shouldUpdateChatRoomStatusSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        boolean newStatus = false;
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, 1L, "테스트 채팅방", true);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        
        // When
        chatRoomService.updateChatRoomStatus(chatRoomId, newStatus);
        
        // Then
        assertThat(chatRoom.getStatus()).isEqualTo(newStatus);
        
        verify(chatRoomRepository).findById(chatRoomId);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 상태 변경 실패")
    void shouldFailWhenUpdatingNonExistentChatRoomStatus() {
        // Given
        Long chatRoomId = 999L;
        boolean newStatus = false;
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.updateChatRoomStatus(chatRoomId, newStatus))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomNotFound()
        
        verify(chatRoomRepository).findById(chatRoomId);
    }

    // ===== 참여자 확인 테스트 =====

    @Test
    @DisplayName("사용자 채팅방 참여 여부 확인 - 참여 중")
    void shouldReturnTrueWhenUserIsParticipant() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        
        given(chatRoomMapper.isParticipant(chatRoomId, memberId)).willReturn(true);
        
        // When
        boolean result = chatRoomService.isParticipant(chatRoomId, memberId);
        
        // Then
        assertThat(result).isTrue();
        
        verify(chatRoomMapper).isParticipant(chatRoomId, memberId);
    }

    @Test
    @DisplayName("사용자 채팅방 참여 여부 확인 - 미참여")
    void shouldReturnFalseWhenUserIsNotParticipant() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 999L;
        
        given(chatRoomMapper.isParticipant(chatRoomId, memberId)).willReturn(false);
        
        // When
        boolean result = chatRoomService.isParticipant(chatRoomId, memberId);
        
        // Then
        assertThat(result).isFalse();
        
        verify(chatRoomMapper).isParticipant(chatRoomId, memberId);
    }

    // ===== 온라인 사용자 정보 테스트 =====

    @Test
    @DisplayName("채팅방 온라인 사용자 정보 조회 성공")
    void shouldGetOnlineUserInfoSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, 1L, "테스트 채팅방", true);
        int onlineCount = 5;
        Set<Long> onlineUserIds = Set.of(1L, 2L, 3L, 4L, 5L);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(onlineUserService.getOnlineCount(chatRoomId)).willReturn(onlineCount);
        given(onlineUserService.getOnlineUsers(chatRoomId)).willReturn(onlineUserIds);
        
        // When
        Map<String, Object> result = chatRoomService.getOnlineUserInfo(chatRoomId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("chatRoomId")).isEqualTo(chatRoomId);
        assertThat(result.get("onlineCount")).isEqualTo(onlineCount);
        assertThat(result.get("onlineUserIds")).isEqualTo(onlineUserIds);
        assertThat(result.get("timestamp")).isNotNull();
        
        verify(chatRoomRepository).findById(chatRoomId);
        verify(onlineUserService).getOnlineCount(chatRoomId);
        verify(onlineUserService).getOnlineUsers(chatRoomId);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방의 온라인 사용자 정보 조회 실패")
    void shouldFailWhenGettingOnlineUserInfoForNonExistentRoom() {
        // Given
        Long chatRoomId = 999L;
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatRoomService.getOnlineUserInfo(chatRoomId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomNotFound()
        
        verify(chatRoomRepository).findById(chatRoomId);
        verify(onlineUserService, never()).getOnlineCount(any());
        verify(onlineUserService, never()).getOnlineUsers(any());
    }

    // ===== Helper 메서드들 =====
    
    private ChatRoomCreateRequestDto createChatRoomCreateRequest() {
        return new ChatRoomCreateRequestDto(1L, "공연토론방", (short) 50);
    }
    
    private Performance createTestPerformance(Long id) {
        // Performance 엔티티의 실제 구조에 맞춰 수정 필요
        return Performance.builder()
                .id(id)
                .title("테스트 공연")
                .build();
    }
    
    private ChatRoom createTestChatRoom(Long id, Long performanceId, String name, Boolean status) {
        return ChatRoom.builder()
                .id(id)
                .performanceId(performanceId)
                .name(name)
                .status(status)
                .maxParticipants((short) 50)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    private ChatRoomResponseDto createChatRoomResponseDto() {
        return ChatRoomResponseDto.builder()
                .chatRoomId(1L)
                .performanceId(1L)
                .name("공연토론방")
                .status(true)
                .maxParticipants((short) 50)
                .participantCount(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
