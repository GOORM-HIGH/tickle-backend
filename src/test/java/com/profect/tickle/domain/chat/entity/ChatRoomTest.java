package com.profect.tickle.domain.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChatRoom Entity 단위 테스트")
class ChatRoomTest {

    @Test
    @DisplayName("TC-CHATROOM-013: 채팅방 참여자 수 관리 - 채팅방 참여자 수를 증가/감소시킨다")
    void shouldReturnTrueWhenCanJoin() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 50);
        int currentParticipantCount = 10;
        
        // When
        boolean canJoin = chatRoom.canJoin(currentParticipantCount);
        
        // Then
        assertThat(canJoin).isTrue();
    }

    @Test
    @DisplayName("TC-CHATROOM-012: 채팅방 상태 변경 - 유효한 ChatRoom 엔티티의 상태를 변경한다")
    void shouldReturnFalseWhenRoomIsInactive() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(false, (short) 50);
        int currentParticipantCount = 10;
        
        // When
        boolean canJoin = chatRoom.canJoin(currentParticipantCount);
        
        // Then
        assertThat(canJoin).isFalse();
    }

    @Test
    @DisplayName("TC-CHATROOM-014: 채팅방 정원 초과 체크 - 정원이 가득 찬 채팅방에 참여를 시도한다")
    void shouldReturnFalseWhenRoomIsFull() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 30);
        int currentParticipantCount = 30; // 정원과 동일
        
        // When
        boolean canJoin = chatRoom.canJoin(currentParticipantCount);
        
        // Then
        assertThat(canJoin).isFalse();
    }

    @Test
    @DisplayName("TC-CHATROOM-014-1: 채팅방 정원 초과 체크 - 정원을 초과한 채팅방에 참여를 시도한다")
    void shouldReturnFalseWhenParticipantsExceedLimit() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 30);
        int currentParticipantCount = 35; // 정원 초과
        
        // When
        boolean canJoin = chatRoom.canJoin(currentParticipantCount);
        
        // Then
        assertThat(canJoin).isFalse();
    }

    @Test
    @DisplayName("ChatRoom.isActive() - 활성 상태 확인 (true)")
    void shouldReturnTrueWhenRoomIsActive() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 50);
        
        // When
        boolean isActive = chatRoom.isActive();
        
        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("ChatRoom.isActive() - 활성 상태 확인 (false)")
    void shouldReturnFalseWhenCheckingInactiveRoom() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(false, (short) 50);
        
        // When
        boolean isActive = chatRoom.isActive();
        
        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("ChatRoom.updateStatus() - 상태 변경 및 타임스탬프 업데이트")
    void shouldUpdateStatusAndTimestamp() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 50);
        Instant originalUpdatedAt = chatRoom.getUpdatedAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        chatRoom.updateStatus(false);
        
        // Then
        assertThat(chatRoom.getStatus()).isFalse();
        assertThat(chatRoom.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("ChatRoom.updateTimestamp() - 타임스탬프만 업데이트")
    void shouldUpdateTimestampOnly() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 50);
        Instant originalUpdatedAt = chatRoom.getUpdatedAt();
        
        // 시간 차이를 만들기 위해 잠시 대기
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        chatRoom.updateTimestamp();
        
        // Then
        assertThat(chatRoom.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(chatRoom.getStatus()).isTrue(); // 상태는 변경되지 않음
    }

    @Test
    @DisplayName("ChatRoom 엔티티 빌더 패턴 테스트")
    void shouldCreateChatRoomWithBuilder() {
        // Given
        Long performanceId = 1L;
        String name = "공연토론방";
        Boolean status = true;
        Short maxParticipants = 100;
        Instant now = Instant.now();
        
        // When
        ChatRoom chatRoom = ChatRoom.builder()
                .performanceId(performanceId)
                .name(name)
                .status(status)
                .maxParticipants(maxParticipants)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        // Then
        assertThat(chatRoom.getPerformanceId()).isEqualTo(performanceId);
        assertThat(chatRoom.getName()).isEqualTo(name);
        assertThat(chatRoom.getStatus()).isEqualTo(status);
        assertThat(chatRoom.getMaxParticipants()).isEqualTo(maxParticipants);
        assertThat(chatRoom.getCreatedAt()).isEqualTo(now);
        assertThat(chatRoom.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ChatRoom 경계값 테스트 - 정원 1명인 경우")
    void shouldHandleMinimumCapacity() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 1);
        
        // When & Then
        assertThat(chatRoom.canJoin(0)).isTrue();  // 0명 -> 참여 가능
        assertThat(chatRoom.canJoin(1)).isFalse(); // 1명 -> 참여 불가 (정원 초과)
    }

    @Test
    @DisplayName("ChatRoom 경계값 테스트 - 정원 직전인 경우")
    void shouldHandleCapacityBoundary() {
        // Given
        ChatRoom chatRoom = createTestChatRoom(true, (short) 50);
        
        // When & Then
        assertThat(chatRoom.canJoin(49)).isTrue();  // 49명 -> 참여 가능
        assertThat(chatRoom.canJoin(50)).isFalse(); // 50명 -> 참여 불가 (정원 동일)
        assertThat(chatRoom.canJoin(51)).isFalse(); // 51명 -> 참여 불가 (정원 초과)
    }

    // === Helper 메서드들 ===
    
    private ChatRoom createTestChatRoom(Boolean status, Short maxParticipants) {
        return ChatRoom.builder()
                .performanceId(1L)
                .name("테스트 채팅방")
                .status(status)
                .maxParticipants(maxParticipants)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
