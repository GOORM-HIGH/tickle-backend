package com.profect.tickle.domain.notification.entity;

import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "notification_title", length = 20, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_is_read", nullable = false)
    private Status status;

    @Column(name = "notification_content", length = 100, nullable = false)
    private String content;

    @Column(name = "notification_created_at", nullable = false)
    private LocalDateTime createdAt;
}
