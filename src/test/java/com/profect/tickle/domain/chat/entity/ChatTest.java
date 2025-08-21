package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Chat Entity 단위 테스트")
class ChatTest {

    @Test
    @DisplayName("TC-ENTITY-001: Chat.editContent() - 메시지 내용 수정")
    void shouldEditContentSuccessfully() {
        // Given
        Chat chat = createTestChat("원본 메시지");
        String newContent = "수정된 내용";
        
        // When
        chat.editContent(newContent);
        
        // Then
        assertThat(chat.getContent()).isEqualTo(newContent);
        assertThat(chat.getIsEdited()).isTrue();
        assertThat(chat.getEditedAt()).isNotNull();
        assertThat(chat.getEditedAt()).isAfter(chat.getCreatedAt());
    }

    @Test
    @DisplayName("TC-ENTITY-002: Chat.markAsDeleted() - 메시지 삭제 처리")
    void shouldMarkAsDeletedSuccessfully() {
        // Given
        Chat chat = createTestChat("삭제할 메시지");
        
        // When
        chat.markAsDeleted();
        
        // Then
        assertThat(chat.getIsDeleted()).isTrue();
        assertThat(chat.getContent()).isEqualTo("삭제된 메시지입니다");
    }

    @Test
    @DisplayName("TC-ENTITY-003: Chat.canEdit() - 수정 가능 여부 확인 (정상)")
    void shouldReturnTrueWhenCanEdit() {
        // Given
        Chat chat = Chat.builder()
                .member(createTestMember())
                .chatRoomId(1L)
                .messageType(ChatMessageType.TEXT)
                .content("수정 가능한 메시지")
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
        
        // When
        boolean canEdit = chat.canEdit();
        
        // Then
        assertThat(canEdit).isTrue();
    }

    @Test
    @DisplayName("TC-ENTITY-003-1: Chat.canEdit() - 삭제된 메시지는 수정 불가")
    void shouldReturnFalseWhenMessageIsDeleted() {
        // Given
        Chat chat = Chat.builder()
                .member(createTestMember())
                .chatRoomId(1L)
                .messageType(ChatMessageType.TEXT)
                .content("삭제된 메시지")
                .isDeleted(true)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
        
        // When
        boolean canEdit = chat.canEdit();
        
        // Then
        assertThat(canEdit).isFalse();
    }

    @Test
    @DisplayName("TC-ENTITY-003-2: Chat.canEdit() - 발신자 상태가 false면 수정 불가")
    void shouldReturnFalseWhenSenderStatusIsFalse() {
        // Given
        Chat chat = Chat.builder()
                .member(createTestMember())
                .chatRoomId(1L)
                .messageType(ChatMessageType.TEXT)
                .content("발신자 상태 false 메시지")
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(false)
                .createdAt(Instant.now())
                .build();
        
        // When
        boolean canEdit = chat.canEdit();
        
        // Then
        assertThat(canEdit).isFalse();
    }

    @Test
    @DisplayName("TC-ENTITY-004: Chat.canDelete() - 삭제 가능 여부 확인 (정상)")
    void shouldReturnTrueWhenCanDelete() {
        // Given
        Chat chat = createTestChat("삭제 가능한 메시지");
        
        // When
        boolean canDelete = chat.canDelete();
        
        // Then
        assertThat(canDelete).isTrue();
    }

    @Test
    @DisplayName("TC-ENTITY-004-1: Chat.canDelete() - 이미 삭제된 메시지는 삭제 불가")
    void shouldReturnFalseWhenAlreadyDeleted() {
        // Given
        Chat chat = createTestChat("이미 삭제된 메시지");
        chat.markAsDeleted();
        
        // When
        boolean canDelete = chat.canDelete();
        
        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("Chat 엔티티 빌더 패턴 테스트")
    void shouldCreateChatWithBuilder() {
        // Given
        Member member = createTestMember();
        Long chatRoomId = 1L;
        ChatMessageType messageType = ChatMessageType.TEXT;
        String content = "테스트 메시지";
        
        // When
        Chat chat = Chat.builder()
                .member(member)
                .chatRoomId(chatRoomId)
                .messageType(messageType)
                .content(content)
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
        
        // Then
        assertThat(chat.getMember()).isEqualTo(member);
        assertThat(chat.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(chat.getMessageType()).isEqualTo(messageType);
        assertThat(chat.getContent()).isEqualTo(content);
        assertThat(chat.getIsDeleted()).isFalse();
        assertThat(chat.getIsEdited()).isFalse();
        assertThat(chat.getSenderStatus()).isTrue();
        assertThat(chat.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 메시지 생성 테스트")
    void shouldCreateFileMessage() {
        // Given
        Member member = createTestMember();
        String filePath = "/uploads/chat/2025/01/31/test-file.txt";
        String fileName = "test-file.txt";
        Integer fileSize = 1024;
        String fileType = "text/plain";
        
        // When
        Chat fileMessage = Chat.builder()
                .member(member)
                .chatRoomId(1L)
                .messageType(ChatMessageType.FILE)
                .content("파일을 업로드했습니다.")
                .filePath(filePath)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileType(fileType)
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
        
        // Then
        assertThat(fileMessage.getMessageType()).isEqualTo(ChatMessageType.FILE);
        assertThat(fileMessage.getFilePath()).isEqualTo(filePath);
        assertThat(fileMessage.getFileName()).isEqualTo(fileName);
        assertThat(fileMessage.getFileSize()).isEqualTo(fileSize);
        assertThat(fileMessage.getFileType()).isEqualTo(fileType);
    }

    // === Helper 메서드들 ===
    
    private Chat createTestChat(String content) {
        return Chat.builder()
                .member(createTestMember())
                .chatRoomId(1L)
                .messageType(ChatMessageType.TEXT)
                .content(content)
                .isDeleted(false)
                .isEdited(false)
                .senderStatus(true)
                .createdAt(Instant.now())
                .build();
    }
    
    private Member createTestMember() {
        return Member.builder()
                .id(1L)
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .memberRole(MemberRole.MEMBER)
                .build();
    }
}
