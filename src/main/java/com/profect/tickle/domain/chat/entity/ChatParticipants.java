package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "chat_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatParticipants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_participants_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "chat_participants_joined_at", nullable = false)
    private Instant joinedAt;  // ✅ UTC 시간 적용

    @Column(name = "chat_participants_status", nullable = false)
    private Boolean status;  // true: 참여중, false: 나감

    @Column(name = "chat_participants_last_read_at")
    private Instant lastReadAt;  // ✅ UTC 시간 적용

    @Column(name = "chat_participants_last_read_message_id")  // ✅ 컬럼명 수정
    private Long lastReadMessageId;

    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();  // UTC로 저장
    }

    @PreUpdate
    protected void onUpdate() {
        // 읽음 처리 시 lastReadAt 자동 업데이트
        if (lastReadMessageId != null) {
            lastReadAt = Instant.now();
        }
    }
}
