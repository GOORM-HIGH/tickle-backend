package com.profect.tickle.domain.notification.integration;

import com.profect.tickle.domain.notification.controller.NotificationController;
import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.global.response.ResultResponse;
import com.profect.tickle.testsecurity.WithMockMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/sql/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationIntegrationTest {

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager em;

    @Test
    @DisplayName("알림 조회 통합테스트 - 로그인 사용자의 최신 알림 목록 반환")
    @WithMockMember(id = 1, email = "user1@test.com")
    void getNotificationList_success() {
        // when
        ResultResponse<List<NotificationResponseDto>> resp = notificationController.getNotificationList(10);

        // then
        assertThat(resp).isNotNull();
        assertThat(resp.getData()).isNotNull();

        assertThat(resp.getData()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(resp.getData())
                .extracting(NotificationResponseDto::getTitle)
                .contains("알림 1", "알림 2", "알림 3");
    }

    @Test
    @DisplayName("알림 읽음 처리 통합테스트 - 상태가 읽음(8)으로 변경")
    @WithMockMember(id = 1, email = "user1@test.com")
    void markAsRead_success() {
        // given
        Long notificationId = 1000L; // data.sql: 상태 7(안읽음)

        // when
        notificationController.markAsRead(notificationId);

        em.flush();

        // then
        Integer statusId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM notification WHERE notification_id = ?",
                Integer.class,
                notificationId
        );

        assertThat(statusId).isEqualTo(8);
    }
}
