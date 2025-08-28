package com.profect.tickle.domain.notification.unit.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.service.MemberService;
import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.domain.notification.mapper.NotificationMapper;
import com.profect.tickle.domain.notification.mapper.NotificationTemplateMapper;
import com.profect.tickle.domain.notification.repository.NotificationRepository;
import com.profect.tickle.domain.notification.repository.SseRepository;
import com.profect.tickle.domain.notification.service.NotificationService;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.domain.performance.service.PerformanceService;
import com.profect.tickle.domain.reservation.service.ReservationService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    // SUT
    @InjectMocks
    private NotificationService notificationService;

    // 실제로 markAsRead에서 쓰는 의존성
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private StatusProvider statusProvider;

    // 생성자 주입을 위해 필요하지만 이 테스트에서는 사용하지 않는 의존성들
    @Mock
    private MemberService memberService;
    @Mock
    private NotificationTemplateService notificationTemplateService;
    @Mock
    private PerformanceService performanceService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SseRepository sseRepository;

    @Test
    @DisplayName("알림 읽음 처리: 자신의 알림이면 읽음 상태로 변경한다.")
    void markAsRead_success() {
        // given
        Long notificationId = 1L;
        Long memberId = 10L;

        Notification notification = mock(Notification.class);
        Member receivedMember = mock(Member.class);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));
        given(notification.getReceivedMember()).willReturn(receivedMember);
        given(receivedMember.getId()).willReturn(memberId);

        Status readStatus = mock(Status.class);
        given(statusProvider.provide(StatusIds.Notification.READ)).willReturn(readStatus);

        // when
        notificationService.markAsRead(notificationId, memberId);

        // then
        then(notificationRepository).should(times(1)).findById(notificationId);
        then(statusProvider).should(times(1)).provide(StatusIds.Notification.READ);
        then(notification).should(times(1)).markAsRead(readStatus);
        then(statusProvider).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리: 알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생합니다.")
    void markAsRead_notFound() {
        // given
        Long notificationId = 999L;
        Long memberId = 10L;
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> notificationService.markAsRead(notificationId, memberId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        then(statusProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 읽음 처리: 다른 사용자의 알림이면 NOTIFICATION_ACCESS_DENIED 예외가 발생합니다.")
    void markAsRead_accessDenied() {
        // given
        Long notificationId = 1L;
        Long myId = 10L;
        Long otherId = 77L;

        Notification notification = mock(Notification.class);
        Member receiver = mock(Member.class);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));
        given(notification.getReceivedMember()).willReturn(receiver);
        given(receiver.getId()).willReturn(otherId); // 내가 아님

        // when
        Throwable thrown = catchThrowable(() -> notificationService.markAsRead(notificationId, myId));

        // then
        assertThat(thrown)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);

        then(statusProvider).shouldHaveNoInteractions();
        then(notification).should(never()).markAsRead(any());
    }
}
