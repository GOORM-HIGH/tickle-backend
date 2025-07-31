package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor  // ✅ 추가!
@Builder             // ✅ 추가!
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_message_type", nullable = false)
    private ChatMessageType messageType;

    @Column(name = "chat_content", length = 255, nullable = false)
    private String content;

    @Column(name = "chat_file_path", length = 255)
    private String filePath;

    @Column(name = "chat_file_name", length = 255)
    private String fileName;

    @Column(name = "chat_file_size")
    private Integer fileSize;

    @Column(name = "chat_file_type", length = 100)
    private String fileType;

    @Column(name = "chat_is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "chat_is_edited", nullable = false)
    private Boolean isEdited;

    @Column(name = "chat_edited_at")
    private Instant editedAt;

    @Column(name = "chat_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "chat_sender_status", nullable = false)
    private Boolean senderStatus; // true: 받음, false: 안 받음

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();  // 항상 UTC로 저장
    }

    @PreUpdate
    protected void onUpdate() {
        // 수정 시에만 editedAt 업데이트
        if (isEdited != null && isEdited) {
            editedAt = Instant.now();
        }
    }

    public void editContent(String newContent) {
        this.content = newContent;
        this.isEdited = true;
        this.editedAt = Instant.now();
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.content = "삭제된 메시지입니다"; // 내용 마스킹
    }

    public boolean canEdit() {
        return !isDeleted && senderStatus;
    }

    public boolean canDelete() {
        return !isDeleted;
    }

}
