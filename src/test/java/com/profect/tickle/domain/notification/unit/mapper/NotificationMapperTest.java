package com.profect.tickle.domain.notification.unit.mapper;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationMapperTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    NotificationMapper notificationMapper;

    @Autowired
    Clock clock;

    @Test
    @DisplayName("회원ID로 수신 알림 목록 조회")
    void getNotificationListByMemberId() {
        // given
        Long memberId = 1L;
        Long templateId = 1L;
        Long statusId = 7L;
        Instant now = Instant.now(clock);

        Member memberRef = em.getReference(Member.class, memberId);
        NotificationTemplate templateRef = em.getReference(NotificationTemplate.class, templateId);
        Status statusRef = em.getReference(Status.class, statusId);

        Notification n1 = createNotification(memberRef, templateRef, statusRef, "알림 제목 1", "알림 내용 1", now);
        em.persist(n1);

        Notification n2 = createNotification(memberRef, templateRef, statusRef, "알림 제목 2", "알림 내용 2", now);
        em.persist(n2);

        em.flush();

        // when
        List<NotificationResponseDto> rows =
                notificationMapper.getNotificationListByMemberId(memberId, 10);

        // then
        assertThat(rows)
                .extracting(NotificationResponseDto::getTitle)
                .contains("알림 1", "알림 2");
    }

    private static Notification createNotification(Member memberRef, NotificationTemplate templateRef, Status statusRef, String title, String content, Instant createdAt) {
        return Notification.builder()
                .receivedMember(memberRef)
                .template(templateRef)
                .status(statusRef)
                .title(title)
                .content(content)
                .createdAt(createdAt)
                .build();
    }
}
