package com.profect.tickle.domain.notification.entity;

import com.profect.tickle.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_template_id")
    private Long id;  // 알림 양식 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 알림 양식 작성자

    @Column(name = "noti_template_title", length = 20, nullable = false)
    private String title;  // 알림 제목

    @Column(name = "noti_template_content", length = 100, nullable = false)
    private String content;  // 알림 내용
}
