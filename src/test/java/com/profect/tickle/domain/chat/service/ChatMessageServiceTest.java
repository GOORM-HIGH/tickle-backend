package com.profect.tickle.domain.chat.service;

import com.profect.tickle.domain.chat.dto.request.ChatMessageSendRequestDto;
import com.profect.tickle.domain.chat.dto.response.ChatMessageResponseDto;

import com.profect.tickle.domain.chat.dto.response.ChatMessageFileDownloadDto;
import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.entity.ChatRoom;

import com.profect.tickle.domain.chat.entity.ChatMessageType;
import com.profect.tickle.domain.chat.repository.ChatRepository;
import com.profect.tickle.domain.chat.repository.ChatRoomRepository;
import com.profect.tickle.domain.chat.repository.ChatParticipantsRepository;
import com.profect.tickle.domain.chat.mapper.ChatMessageMapper;
import com.profect.tickle.domain.file.service.FileService;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import com.profect.tickle.domain.member.repository.MemberRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService 단위 테스트")
class ChatMessageServiceTest {

    @Mock
    private ChatParticipantsRepository chatParticipantsRepository;
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private ChatMessageMapper chatMessageMapper;
    
    @Mock
    private FileService fileService;
    
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private ChatMessageService chatMessageService;

    // ===== 메시지 전송 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-001: 텍스트 메시지 전송 성공")
    void shouldSendTextMessageSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 1L;
        ChatMessageSendRequestDto requestDto = createTextMessageRequest("안녕하세요");
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        Chat savedMessage = createTestChat(1L, sender, chatRoomId, "안녕하세요");
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(true);
        given(chatRepository.save(any(Chat.class))).willReturn(savedMessage);
        
        // When
        ChatMessageResponseDto result = chatMessageService.sendMessage(chatRoomId, senderId, requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("안녕하세요");
        assertThat(result.getMessageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(result.getSenderNickname()).isEqualTo(sender.getNickname());
        
        verify(chatRepository).save(any(Chat.class));
        verify(chatRoomRepository).findById(chatRoomId);
        verify(memberRepository).findById(senderId);
        verify(chatParticipantsRepository).existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender);
    }

    @Test
    @DisplayName("TC-MESSAGE-002: 빈 텍스트 메시지 전송 실패")
    void shouldFailWhenSendingEmptyTextMessage() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 1L;
        ChatMessageSendRequestDto requestDto = createTextMessageRequest("");
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoomId, senderId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatMessageEmptyContent()
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-003: 255자 초과 텍스트 메시지 전송 실패")
    void shouldFailWhenSendingTooLongTextMessage() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 1L;
        String longContent = "a".repeat(256); // 256자
        ChatMessageSendRequestDto requestDto = createTextMessageRequest(longContent);
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoomId, senderId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatMessageTooLong()
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-004: 파일 메시지 전송 성공")
    void shouldSendFileMessageSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 1L;
        ChatMessageSendRequestDto requestDto = createFileMessageRequest();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        Chat savedMessage = createTestFileChat(1L, sender, chatRoomId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(true);
        given(chatRepository.save(any(Chat.class))).willReturn(savedMessage);
        
        // When
        ChatMessageResponseDto result = chatMessageService.sendMessage(chatRoomId, senderId, requestDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessageType()).isEqualTo(ChatMessageType.FILE);
        assertThat(result.getFileName()).isEqualTo("test-file.txt");
        
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-005: 파일 정보 누락으로 파일 메시지 전송 실패")
    void shouldFailWhenSendingFileMessageWithMissingInfo() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 1L;
        ChatMessageSendRequestDto requestDto = ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다.")
                .filePath(null) // 파일 경로 누락
                .fileName(null) // 파일명 누락
                .build();
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoomId, senderId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatMessageMissingFileInfo()
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-006: 비참여자 메시지 전송 실패")
    void shouldFailWhenNonParticipantSendsMessage() {
        // Given
        Long chatRoomId = 1L;
        Long senderId = 999L; // 참여하지 않은 사용자
        ChatMessageSendRequestDto requestDto = createTextMessageRequest("안녕하세요");
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member sender = createTestMember(senderId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(senderId)).willReturn(Optional.of(sender));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, sender)).willReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.sendMessage(chatRoomId, senderId, requestDto))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatNotParticipant()
        
        verify(chatRepository, never()).save(any(Chat.class));
    }

    // ===== 메시지 수정 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-007: 메시지 수정 성공")
    void shouldEditMessageSuccessfully() {
        // Given
        Long messageId = 1L;
        Long editorId = 1L;
        String newContent = "수정된 내용";
        
        Member editor = createTestMember(editorId);
        Chat message = createTestChat(messageId, editor, 1L, "원본 내용");
        
        given(chatRepository.findById(messageId)).willReturn(Optional.of(message));
        
        // When
        ChatMessageResponseDto result = chatMessageService.editMessage(messageId, editorId, newContent);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(message.getContent()).isEqualTo(newContent);
        assertThat(message.getIsEdited()).isTrue();
        assertThat(message.getEditedAt()).isNotNull();
        
        verify(chatRepository).findById(messageId);
    }

    @Test
    @DisplayName("TC-MESSAGE-008: 타인 메시지 수정 시도 실패")
    void shouldFailWhenEditingOthersMessage() {
        // Given
        Long messageId = 1L;
        Long editorId = 2L; // 다른 사용자
        String newContent = "수정 시도";
        
        Member originalAuthor = createTestMember(1L);
        Chat message = createTestChat(messageId, originalAuthor, 1L, "원본 내용");
        
        given(chatRepository.findById(messageId)).willReturn(Optional.of(message));
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.editMessage(messageId, editorId, newContent))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatNotMessageOwner()
        
        verify(chatRepository).findById(messageId);
    }

    @Test
    @DisplayName("TC-MESSAGE-009: 삭제된 메시지 수정 시도 실패")
    void shouldFailWhenEditingDeletedMessage() {
        // Given
        Long messageId = 1L;
        Long editorId = 1L;
        String newContent = "수정 시도";
        
        Member editor = createTestMember(editorId);
        Chat message = createTestChat(messageId, editor, 1L, "원본 내용");
        message.markAsDeleted(); // 메시지 삭제 처리
        
        given(chatRepository.findById(messageId)).willReturn(Optional.of(message));
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.editMessage(messageId, editorId, newContent))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatMessageCannotEdit()
        
        verify(chatRepository).findById(messageId);
    }

    // ===== 메시지 삭제 테스트 =====

    @Test
    @DisplayName("TC-MESSAGE-010: 메시지 삭제 성공")
    void shouldDeleteMessageSuccessfully() {
        // Given
        Long messageId = 1L;
        Long deleterId = 1L;
        
        Member deleter = createTestMember(deleterId);
        Chat message = createTestChat(messageId, deleter, 1L, "삭제할 메시지");
        
        given(chatRepository.findById(messageId)).willReturn(Optional.of(message));
        
        // When
        chatMessageService.deleteMessage(messageId, deleterId);
        
        // Then
        assertThat(message.getIsDeleted()).isTrue();
        assertThat(message.getContent()).isEqualTo("삭제된 메시지입니다");
        
        verify(chatRepository).findById(messageId);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/" + message.getChatRoomId()), any(Object.class));
    }

    @Test
    @DisplayName("TC-MESSAGE-011: 이미 삭제된 메시지 삭제 시도 실패")
    void shouldFailWhenDeletingAlreadyDeletedMessage() {
        // Given
        Long messageId = 1L;
        Long deleterId = 1L;
        
        Member deleter = createTestMember(deleterId);
        Chat message = createTestChat(messageId, deleter, 1L, "이미 삭제된 메시지");
        message.markAsDeleted(); // 이미 삭제됨
        
        given(chatRepository.findById(messageId)).willReturn(Optional.of(message));
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.deleteMessage(messageId, deleterId))
                .isInstanceOf(RuntimeException.class); // ChatExceptions.chatMessageAlreadyDeleted()
        
        verify(chatRepository).findById(messageId);
    }

    // ===== 파일 다운로드 테스트 =====

    @Test
    @DisplayName("TC-FILE-001: 파일 다운로드 정보 조회 성공")
    void shouldGetFileDownloadInfoSuccessfully() {
        // Given
        Long chatRoomId = 1L;
        Long messageId = 1L;
        Long currentMemberId = 1L;
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member member = createTestMember(currentMemberId);
        Chat fileMessage = createTestFileChat(messageId, member, chatRoomId);
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, member)).willReturn(true);
        given(chatRepository.findById(messageId)).willReturn(Optional.of(fileMessage));
        
        // When
        ChatMessageFileDownloadDto result = chatMessageService.getMessageFileForDownload(chatRoomId, messageId, currentMemberId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test-file.txt");
        assertThat(result.getFilePath()).isEqualTo("/uploads/chat/test-file.txt");
        assertThat(result.getFileType()).isEqualTo("text/plain");
        assertThat(result.getFileSize()).isEqualTo(1024);
    }

    @Test
    @DisplayName("TC-FILE-002: 텍스트 메시지 파일 다운로드 시도 실패")
    void shouldFailWhenDownloadingNonFileMessage() {
        // Given
        Long chatRoomId = 1L;
        Long messageId = 1L;
        Long currentMemberId = 1L;
        
        ChatRoom chatRoom = createTestChatRoom(chatRoomId, true);
        Member member = createTestMember(currentMemberId);
        Chat textMessage = createTestChat(messageId, member, chatRoomId, "텍스트 메시지");
        
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(member));
        given(chatParticipantsRepository.existsByChatRoomAndMemberAndStatusTrue(chatRoom, member)).willReturn(true);
        given(chatRepository.findById(messageId)).willReturn(Optional.of(textMessage));
        
        // When & Then
        assertThatThrownBy(() -> chatMessageService.getMessageFileForDownload(chatRoomId, messageId, currentMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일이 첨부되지 않은 메시지입니다");
    }

    // ===== Helper 메서드들 =====
    
    private ChatMessageSendRequestDto createTextMessageRequest(String content) {
        return ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.TEXT)
                .content(content)
                .build();
    }
    
    private ChatMessageSendRequestDto createFileMessageRequest() {
        return ChatMessageSendRequestDto.builder()
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다.")
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileSize(1024)
                .fileType("text/plain")
                .build();
    }
    
    private ChatRoom createTestChatRoom(Long id, Boolean status) {
        return ChatRoom.builder()
                .id(id)
                .performanceId(1L)
                .name("테스트 채팅방")
                .status(status)
                .maxParticipants((short) 50)
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
    
    private Chat createTestChat(Long id, Member member, Long chatRoomId, String content) {
        return Chat.builder()
                .id(id)
                .member(member)
                .chatRoomId(chatRoomId)
                .messageType(ChatMessageType.TEXT)
                .content(content)
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
    }
    
    private Chat createTestFileChat(Long id, Member member, Long chatRoomId) {
        return Chat.builder()
                .id(id)
                .member(member)
                .chatRoomId(chatRoomId)
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다.")
                .filePath("/uploads/chat/test-file.txt")
                .fileName("test-file.txt")
                .fileSize(1024)
                .fileType("text/plain")
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
    }
}
