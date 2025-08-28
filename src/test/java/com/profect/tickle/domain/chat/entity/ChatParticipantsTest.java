package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChatParticipants Entity 단위 테스트")
class ChatParticipantsTest {

    @Test
    @DisplayName("TC-PARTICIPANT-010: ChatParticipants 읽음 처리 - 마지막 읽은 메시지 정보를 업데이트한다")
    void shouldUpdateLastReadMessageSuccessfully() {
        // Given
        ChatParticipants participant = createTestParticipant();
        Long messageId = 10L;
        Instant beforeUpdate = participant.getLastReadAt();
        
        // When
        participant.updateLastReadMessage(messageId);
        
        // Then
        assertThat(participant.getLastReadMessageId()).isEqualTo(messageId);
        assertThat(participant.getLastReadAt()).isNotNull();
        if (beforeUpdate != null) {
            assertThat(participant.getLastReadAt()).isAfter(beforeUpdate);
        }
    }

    @Test
    @DisplayName("TC-PARTICIPANT-009: ChatParticipants 참여 처리 - 유효한 ChatParticipants 엔티티의 참여 상태를 변경한다")
    void shouldRejoinSuccessfully() {
        // Given
        ChatParticipants participant = createTestParticipant();
        participant.leave(); // 먼저 나가기
        
        // When
        participant.rejoin();
        
        // Then
        assertThat(participant.getStatus()).isTrue();
        // joinedAt은 최초 참여 시간을 유지해야 함
    }

    @Test
    @DisplayName("ChatParticipants.leave() - 나가기 처리")
    void shouldLeaveSuccessfully() {
        // Given
        ChatParticipants participant = createTestParticipant();
        
        // When
        participant.leave();
        
        // Then
        assertThat(participant.getStatus()).isFalse();
    }

    @Test
    @DisplayName("ChatParticipants.isActive() - 활성 상태 확인 (true)")
    void shouldReturnTrueWhenActive() {
        // Given
        ChatParticipants participant = createTestParticipant();
        
        // When
        boolean isActive = participant.isActive();
        
        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("ChatParticipants.isActive() - 활성 상태 확인 (false)")
    void shouldReturnFalseWhenInactive() {
        // Given
        ChatParticipants participant = createTestParticipant();
        participant.leave();
        
        // When
        boolean isActive = participant.isActive();
        
        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("ChatParticipants.reactivate() - 재활성화 처리")
    void shouldReactivateSuccessfully() {
        // Given
        ChatParticipants participant = createTestParticipant();
        participant.leave(); // 먼저 나가기
        Instant originalJoinedAt = participant.getJoinedAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        participant.reactivate();
        
        // Then
        assertThat(participant.getStatus()).isTrue();
        assertThat(participant.getJoinedAt()).isAfter(originalJoinedAt); // 재참여 시간 업데이트됨
    }

    @Test
    @DisplayName("ChatParticipants 엔티티 빌더 패턴 테스트")
    void shouldCreateParticipantWithBuilder() {
        // Given
        ChatRoom chatRoom = createTestChatRoom();
        Member member = createTestMember();
        Boolean status = true;
        Instant joinedAt = Instant.now();
        
        // When
        ChatParticipants participant = ChatParticipants.builder()
                .chatRoom(chatRoom)
                .member(member)
                .status(status)
                .joinedAt(joinedAt)
                .build();
        
        // Then
        assertThat(participant.getChatRoom()).isEqualTo(chatRoom);
        assertThat(participant.getMember()).isEqualTo(member);
        assertThat(participant.getStatus()).isEqualTo(status);
        assertThat(participant.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    @DisplayName("읽음 처리 시나리오 테스트")
    void shouldHandleReadStatusCorrectly() {
        // Given
        ChatParticipants participant = createTestParticipant();
        
        // When - 첫 번째 메시지 읽음
        participant.updateLastReadMessage(1L);
        Long firstMessageId = participant.getLastReadMessageId();
        Instant firstReadAt = participant.getLastReadAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When - 두 번째 메시지 읽음
        participant.updateLastReadMessage(5L);
        
        // Then
        assertThat(participant.getLastReadMessageId()).isEqualTo(5L);
        assertThat(participant.getLastReadMessageId()).isNotEqualTo(firstMessageId);
        assertThat(participant.getLastReadAt()).isAfter(firstReadAt);
    }

    @Test
    @DisplayName("참여 -> 나가기 -> 재참여 시나리오 테스트")
    void shouldHandleJoinLeaveRejoinScenario() {
        // Given
        ChatParticipants participant = createTestParticipant();
        assertThat(participant.isActive()).isTrue();
        
        // When - 나가기
        participant.leave();
        
        // Then - 나간 상태 확인
        assertThat(participant.isActive()).isFalse();
        
        // When - 재참여 (rejoin)
        participant.rejoin();
        
        // Then - 재참여 상태 확인
        assertThat(participant.isActive()).isTrue();
    }

    @Test
    @DisplayName("참여 -> 나가기 -> 재활성화 시나리오 테스트")
    void shouldHandleJoinLeaveReactivateScenario() {
        // Given
        ChatParticipants participant = createTestParticipant();
        Instant originalJoinedAt = participant.getJoinedAt();
        
        // When - 나가기
        participant.leave();
        assertThat(participant.isActive()).isFalse();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When - 재활성화 (reactivate)
        participant.reactivate();
        
        // Then - 재활성화 상태 확인
        assertThat(participant.isActive()).isTrue();
        assertThat(participant.getJoinedAt()).isAfter(originalJoinedAt); // 시간 업데이트됨
    }

    @Test
    @DisplayName("null 값 처리 테스트")
    void shouldHandleNullValues() {
        // Given
        ChatParticipants participant = ChatParticipants.builder()
                .chatRoom(createTestChatRoom())
                .member(createTestMember())
                .status(true)
                .joinedAt(Instant.now())
                .lastReadAt(null)           // null 값
                .lastReadMessageId(null)    // null 값
                .build();
        
        // When & Then
        assertThat(participant.getLastReadAt()).isNull();
        assertThat(participant.getLastReadMessageId()).isNull();
        
        // When - 읽음 처리
        participant.updateLastReadMessage(1L);
        
        // Then
        assertThat(participant.getLastReadAt()).isNotNull();
        assertThat(participant.getLastReadMessageId()).isEqualTo(1L);
    }

    // === Helper 메서드들 ===
    
    private ChatParticipants createTestParticipant() {
        return ChatParticipants.builder()
                .chatRoom(createTestChatRoom())
                .member(createTestMember())
                .status(true)
                .joinedAt(Instant.now())
                .build();
    }
    
    private ChatRoom createTestChatRoom() {
        return ChatRoom.builder()
                .id(1L)
                .performanceId(1L)
                .name("테스트 채팅방")
                .status(true)
                .maxParticipants((short) 50)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
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
