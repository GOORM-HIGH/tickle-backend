package com.profect.tickle.domain.notification.unit.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.global.status.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEntityTest {

    @Test
    @DisplayName("markAsRead 메서드가 Status를 변경한다")
    void markAsRead_updatesStatus() {
        // given
        Status unread = Mockito.mock(Status.class);
        Status read   = Mockito.mock(Status.class);

        Member member = Member.builder().id(1L).build();
        NotificationTemplate template = Mockito.mock(NotificationTemplate.class);

        Notification notification = Notification.builder()
                .id(100L)
                .receivedMember(member)
                .template(template)
                .title("테스트 알림")
                .content("내용")
                .status(unread)
                .createdAt(Instant.now())
                .build();

        // when
        notification.markAsRead(read);

        // then
        assertThat(notification.getStatus()).isSameAs(read);
    }

    @Test
    @DisplayName("isForMember 메서드가 해당 회원 여부를 판별한다")
    void isForMember_checksCorrectMember() {
        // given
        Long trueId = 1L;
        Long falseId = 2L;

        Member member = Member.builder().id(trueId).build();
        Status unread = Mockito.mock(Status.class);
        NotificationTemplate template = Mockito.mock(NotificationTemplate.class);

        // when
        Notification notification = Notification.builder()
                .id(200L)
                .receivedMember(member)
                .template(template)
                .title("알림")
                .content("내용")
                .status(unread)
                .createdAt(Instant.now())
                .build();

        // expect
        assertThat(notification.isForMember(trueId)).isTrue();
        assertThat(notification.isForMember(falseId)).isFalse();
    }
}
