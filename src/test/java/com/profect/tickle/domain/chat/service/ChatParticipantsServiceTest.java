package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatRoomJoinRequestDto;
import com.profect.tickle.domain.chat.dto.request.ReadMessageRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatParticipantsResponseDto;
import com.profect.tickle.domain.chat.dto.response.UnreadCountResponseDto;
import com.profect.tickle.domain.chat.entity.ChatParticipants;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.chat.mapper.ChatParticipantsMapper;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatParticipantsService 단위 테스트")
class ChatParticipantsServiceTest {

    @Mock
    private ChatParticipantsRepository chatParticipantsRepository;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private ChatParticipantsMapper chatParticipantsMapper;

    @InjectMocks
    private ChatParticipantsService chatParticipantsService;

    // ===== 채팅방 참여 테스트 =====

    @Test
    @DisplayName("TC-PARTICIPANT-001: 채팅방 첫 참여 성공")
    void shouldJoinChatRoomSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        ChatRoomJoinRequestDto requestDto = new ChatRoomJoinRequestDto();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 50);
        Member member = createTestMember(memberId);
        ChatParticipants savedParticipant = createTestParticipant(1L, chatRoom, member, true);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.empty());
        given(chatParticipantsRepository.countByChatRoomAndStatusTrue(chatRoom)).willReturn(10);
        given(chatParticipantsRepository.save(any(ChatParticipants.class))).willReturn(savedParticipant);
        
        // When
        ChatParticipantsResponseDto result = chatParticipantsService.joinChatRoom(chatRoomId, memberId, requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getStatus()).isTrue();
        
        verify(chatRoomRepository).findById(chatRoomId);
        verify(memberRepository).findById(memberId);
        verify(chatParticipantsRepository).findByChatRoomAndMember(chatRoom, member);
        verify(chatParticipantsRepository).countByChatRoomAndStatusTrue(chatRoom);
        verify(chatParticipantsRepository).save(any(ChatParticipants.class));
    }

    @Test
    @DisplayName("TC-PARTICIPANT-002: 채팅방 재참여 성공")
    void shouldRejoinChatRoomSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        ChatRoomJoinRequestDto requestDto = new ChatRoomJoinRequestDto();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 50);
        Member member = createTestMember(memberId);
        ChatParticipants existingParticipant = createTestParticipant(1L, chatRoom, member, false); // 비활성 상태
        ChatParticipants reactivatedParticipant = createTestParticipant(1L, chatRoom, member, true);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.of(existingParticipant));
        given(chatParticipantsRepository.save(existingParticipant)).willReturn(reactivatedParticipant);
        
        // When
        ChatParticipantsResponseDto result = chatParticipantsService.joinChatRoom(chatRoomId, memberId, requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isTrue();
        
        verify(chatParticipantsRepository).save(existingParticipant);
    }

    @Test
    @DisplayName("TC-PARTICIPANT-003: 이미 참여 중인 사용자의 중복 참여")
    void shouldReturnExistingParticipantWhenAlreadyJoined() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        ChatRoomJoinRequestDto requestDto = new ChatRoomJoinRequestDto();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 50);
        Member member = createTestMember(memberId);
        ChatParticipants existingParticipant = createTestParticipant(1L, chatRoom, member, true); // 이미 활성 상태
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.of(existingParticipant));
        
        // When
        ChatParticipantsResponseDto result = chatParticipantsService.joinChatRoom(chatRoomId, memberId, requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isTrue();
        
        verify(chatParticipantsRepository, never()).save(any(ChatParticipants.class)); // 새로 저장하지 않음
    }

    @Test
    @DisplayName("TC-PARTICIPANT-004: 정원 초과로 참여 실패")
    void shouldFailWhenChatRoomIsFull() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        ChatRoomJoinRequestDto requestDto = new ChatRoomJoinRequestDto();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 30); // 정원 30명
        Member member = createTestMember(memberId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.empty());
        given(chatParticipantsRepository.countByChatRoomAndStatusTrue(chatRoom)).willReturn(30); // 정원 가득 참
        
        // When & Then
        assertThatThrownBy(() -> chatParticipantsService.joinChatRoom(chatRoomId, memberId, requestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅방 정원이 가득 찼습니다.");
        
        verify(chatParticipantsRepository, never()).save(any(ChatParticipants.class));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 참여 시도 실패")
    void shouldFailWhenJoiningNonExistentChatRoom() {
        // Given
        Long chatRoomId = 999L;
        Long memberId = 1L;
        ChatRoomJoinRequestDto requestDto = new ChatRoomJoinRequestDto();
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatParticipantsService.joinChatRoom(chatRoomId, memberId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatRoomNotFound()
        
        verify(chatParticipantsRepository, never()).save(any(ChatParticipants.class));
    }

    // ===== 채팅방 나가기 테스트 =====

    @Test
    @DisplayName("TC-PARTICIPANT-005: 채팅방 나가기 성공")
    void shouldLeaveChatRoomSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 50);
        Member member = createTestMember(memberId);
        ChatParticipants participant = createTestParticipant(1L, chatRoom, member, true);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.of(participant));
        
        // When
        chatParticipantsService.leaveChatRoom(chatRoomId, memberId);
        
        // Then
        verify(chatRoomRepository).findById(chatRoomId);
        verify(memberRepository).findById(memberId);
        verify(chatParticipantsRepository).findByChatRoomAndMember(chatRoom, member);
    }

    @Test
    @DisplayName("TC-PARTICIPANT-006: 존재하지 않는 참여자 나가기 시도 실패")
    void shouldFailWhenLeavingAsNonParticipant() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 999L; // 참여하지 않은 사용자
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, (short) 50);
        Member member = createTestMember(memberId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.findByChatRoomAndMember(chatRoom, member)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> chatParticipantsService.leaveChatRoom(chatRoomId, memberId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatParticipantNotFound()
        
        verify(chatRoomRepository).findById(chatRoomId);
        verify(memberRepository).findById(memberId);
        verify(chatParticipantsRepository).findByChatRoomAndMember(chatRoom, member);
    }

    // ===== 읽음 처리 테스트 =====

    @Test
    @DisplayName("TC-READ-001: 메시지 읽음 처리 성공")
    void shouldMarkAsReadSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        ReadMessageRequestDto requestDto = new ReadMessageRequestDto(10L);
        
        given(chatParticipantsMapper.updateLastReadMessage(eq(chatRoomId), eq(memberId), eq(10L), any(Instant.class)))
                .willReturn(1); // 1개 행 업데이트됨
        
        // When
        chatParticipantsService.markAsRead(chatRoomId, memberId, requestDto);
        
        // Then
        verify(chatParticipantsMapper).updateLastReadMessage(eq(chatRoomId), eq(memberId), eq(10L), any(Instant.class));
    }

    @Test
    @DisplayName("TC-READ-002: 비참여자 읽음 처리 실패")
    void shouldFailWhenMarkingAsReadForNonParticipant() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 999L; // 참여하지 않은 사용자
        ReadMessageRequestDto requestDto = new ReadMessageRequestDto(10L);
        
        given(chatParticipantsMapper.updateLastReadMessage(eq(chatRoomId), eq(memberId), eq(10L), any(Instant.class)))
                .willReturn(0); // 업데이트된 행 없음
        
        // When & Then
        assertThatThrownBy(() -> chatParticipantsService.markAsRead(chatRoomId, memberId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatParticipantNotFound()
        
        verify(chatParticipantsMapper).updateLastReadMessage(eq(chatRoomId), eq(memberId), eq(10L), any(Instant.class));
    }

    // ===== 읽음 상태 조회 테스트 =====

    @Test
    @DisplayName("TC-READ-003: 읽음 상태 조회 성공")
    void shouldGetUnreadCountSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 1L;
        UnreadCountResponseDto expectedResponse = UnreadCountResponseDto.builder()
                .lastReadMessageId(5L)
                .unreadCount(3)
                .build();
        
        given(chatParticipantsMapper.getReadStatus(chatRoomId, memberId)).willReturn(expectedResponse);
        
        // When
        UnreadCountResponseDto result = chatParticipantsService.getUnreadCount(chatRoomId, memberId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLastReadMessageId()).isEqualTo(5L);
        assertThat(result.getUnreadCount()).isEqualTo(3);
        
        verify(chatParticipantsMapper).getReadStatus(chatRoomId, memberId);
    }

    @Test
    @DisplayName("비참여자 읽음 상태 조회 실패")
    void shouldFailWhenGettingUnreadCountForNonParticipant() {
        // Given
        Long chatRoomId = 1L;
        Long memberId = 999L;
        
        given(chatParticipantsMapper.getReadStatus(chatRoomId, memberId)).willReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> chatParticipantsService.getUnreadCount(chatRoomId, memberId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatParticipantNotFound()
        
        verify(chatParticipantsMapper).getReadStatus(chatRoomId, memberId);
    }

    // ===== 참여자 목록 조회 테스트 =====

    @Test
    @DisplayName("채팅방 참여자 목록 조회 성공")
    void shouldGetParticipantsByRoomIdSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        List<ChatParticipantsResponseDto> expectedParticipants = List.of(
                createParticipantResponseDto(1L, 1L, 1L),
                createParticipantResponseDto(2L, 1L, 2L),
                createParticipantResponseDto(3L, 1L, 3L)
        );
        
        given(chatParticipantsMapper.findParticipantsByRoomId(chatRoomId)).willReturn(expectedParticipants);
        
        // When
        List<ChatParticipantsResponseDto> result = chatParticipantsService.getParticipantsByRoomId(chatRoomId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getChatRoomId()).isEqualTo(chatRoomId);
        
        verify(chatParticipantsMapper).findParticipantsByRoomId(chatRoomId);
    }

    @Test
    @DisplayName("내가 참여한 채팅방 목록 조회 성공")
    void shouldGetMyChatRoomsSuccessfully() {
        // Given
        Long memberId = 1L;
        List<ChatParticipantsResponseDto> expectedChatRooms = List.of(
                createParticipantResponseDto(1L, 1L, memberId),
                createParticipantResponseDto(2L, 2L, memberId)
        );
        
        given(chatParticipantsMapper.findMyChatRooms(memberId)).willReturn(expectedChatRooms);
        
        // When
        List<ChatParticipantsResponseDto> result = chatParticipantsService.getMyChatRooms(memberId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemberId()).isEqualTo(memberId);
        assertThat(result.get(1).getMemberId()).isEqualTo(memberId);
        
        verify(chatParticipantsMapper).findMyChatRooms(memberId);
    }

    // ===== Helper 메서드들 =====
    
    private ChatRoom createTestChatRoom(Long id, Short maxParticipants) {
        return ChatRoom.builder()
                .id(id)
                .performanceId(1L)
                .name("테스트 채팅방")
                .status(true)
                .maxParticipants(maxParticipants)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    private Member createTestMember(Long id) {
        return Member.builder()
                .id(id)
                .email("test" + id + "@example.com")
                .password("password123")
                .nickname("테스터" + id)
                .memberRole(MemberRole.MEMBER)
                .build();
    }
    
    private ChatParticipants createTestParticipant(Long id, ChatRoom chatRoom, Member member, Boolean status) {
        return ChatParticipants.builder()
                .id(id)
                .chatRoom(chatRoom)
                .member(member)
                .status(status)
                .joinedAt(Instant.now())
                .build();
    }
    
    private ChatParticipantsResponseDto createParticipantResponseDto(Long id, Long chatRoomId, Long memberId) {
        return ChatParticipantsResponseDto.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .memberId(memberId)
                .status(true)
                .joinedAt(Instant.now())
                .build();
    }
}
