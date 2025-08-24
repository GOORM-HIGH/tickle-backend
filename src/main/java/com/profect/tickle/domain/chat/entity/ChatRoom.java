package com.profect.tickle.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Entity
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;  // 단순한 FK

    @Column(name = "chat_room_name", length = 20, nullable = false)
    private String name;

    @Column(name = "chat_room_status", nullable = false)
    private Boolean status;

    @Column(name = "chat_room_max_participants", nullable = false)
    private Short maxParticipants;

    @Column(name = "chat_room_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "chat_room_updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ✅ 비즈니스 메서드들
    public void updateStatus(boolean status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status;
    }

    public boolean canJoin(int currentParticipantCount) {
        return isActive() && currentParticipantCount < maxParticipants;
    }

    // ✅ 추가: 외부에서 호출 가능한 public 메서드
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
