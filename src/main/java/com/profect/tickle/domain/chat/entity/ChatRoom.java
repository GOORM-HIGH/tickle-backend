package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.performance.entity.Performance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "chat_room_name", length = 20, nullable = false)
    private String name;

    @Column(name = "chat_room_status", nullable = false)
    private Boolean status;  // true: 열림, false: 종료

    @Column(name = "chat_room_max_participants", nullable = false)
    private Short maxParticipants; // default 100, 최대 200명 예상

    @Column(name = "chat_room_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "chat_room_updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();  // UTC로 저장
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();  // UTC로 저장
    }

}
