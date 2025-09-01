package com.profect.tickle.domain.notification.unit.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateEntityTest {

    @Test
    @DisplayName("Builder를 통해 NotificationTemplate 엔티티를 생성할 수 있다")
    void create_withBuilder() {
        // given
        Member maker = Member.builder()
                .id(1L)
                .email("tester@example.com")
                .nickname("tester")
                .build();

        // when
        NotificationTemplate template = NotificationTemplate.builder()
                .id(10L)
                .maker(maker)
                .title("알림 제목")
                .content("알림 내용입니다.")
                .build();

        // then
        assertThat(template.getId()).isEqualTo(10L);
        assertThat(template.getMaker()).isSameAs(maker);
        assertThat(template.getTitle()).isEqualTo("알림 제목");
        assertThat(template.getContent()).isEqualTo("알림 내용입니다.");
    }

    @Test
    @DisplayName("toString()이 주요 필드를 포함한다")
    void toString_containsFields() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .title("공지")
                .content("내용")
                .build();

        String toString = template.toString();

        assertThat(toString).contains("공지");
        assertThat(toString).contains("내용");
    }
}
