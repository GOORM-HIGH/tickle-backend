package com.profect.tickle.domain.notification.entity;

import com.profect.tickle.domain.member.entity.Member;
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
    @Column(name = "notification_template_id")
    private Long id;  // 알림 양식 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_template_maker_id", nullable = false)
    private Member maker;  // 알림 양식 작성자

    @Column(name = "notification_template_title", length = 20, nullable = false)
    private String title;  // 알림 제목

    @Column(name = "notification_template_content", length = 100, nullable = false)
    private String content;  // 알림 내용
}
