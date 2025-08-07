package com.profect.tickle.domain.notification.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_received_member_id", nullable = false)
    private Member receivedMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_template_id", nullable = false)
    private NotificationTemplate template;

    @Column(name = "notification_title", nullable = false, length = 100)
    private String title;

    @Column(name = "notification_content", nullable = false, length = 255)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "notification_created_at", nullable = false)
    private Instant createdAt;

    public void markAsRead(@NonNull Status isReadStatus) {
        this.status = isReadStatus;
    }
}
