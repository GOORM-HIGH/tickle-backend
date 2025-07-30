package com.profect.tickle.domain.chat.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
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
    private LocalDateTime editedAt;

    @Column(name = "chat_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "chat_sender_status", nullable = false)
    private Boolean senderStatus; // true: 받음, false: 안 받음
}
