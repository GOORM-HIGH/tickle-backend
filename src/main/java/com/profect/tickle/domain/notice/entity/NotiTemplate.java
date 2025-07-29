package com.profect.tickle.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "noti_template")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotiTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_template_id")
    private Long id;

    @Column(name = "noti_template_title", length = 20, nullable = false)
    private String title;

    @Column(name = "noti_template_content", length = 100, nullable = false)
    private String content;
}
