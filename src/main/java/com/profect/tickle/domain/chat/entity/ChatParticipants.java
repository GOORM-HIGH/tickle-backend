package com.profect.tickle.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "chat_participants_joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "chat_participants_status", nullable = false)
    private Boolean isActive; // true: 참여중, false: 나감

    @Column(name = "chat_participants_last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "chat_participants_last_read_id")
    private Long lastReadMessageId;
}
