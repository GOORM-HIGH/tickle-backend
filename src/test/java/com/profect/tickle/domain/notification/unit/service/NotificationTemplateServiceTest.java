package com.profect.tickle.domain.notification.unit.service;

import com.profect.tickle.domain.notification.entity.NotificationTemplate;
import com.profect.tickle.domain.notification.mapper.NotificationTemplateMapper;
import com.profect.tickle.domain.notification.service.NotificationTemplateService;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;

    private NotificationTemplateService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateService(notificationTemplateMapper);
    }

    @Test
    @DisplayName("템플릿 ID로 조회 성공 시 엔티티를 반환한다")
    void getNotificationTemplateById_success() {
        // given
        Long templateId = 1L;

        // 엔티티 내용은 테스트에 중요하지 않으니 mock 객체로 대체
        NotificationTemplate template = mock(NotificationTemplate.class);

        when(notificationTemplateMapper.findById(templateId))
                .thenReturn(Optional.of(template));

        // when
        NotificationTemplate result = service.getNotificationTemplateById(templateId);

        // then
        assertThat(result).isSameAs(template);

        // 호출 검증 + 전달 파라미터 검증
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationTemplateMapper, times(1)).findById(idCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(templateId);
        verifyNoMoreInteractions(notificationTemplateMapper);
    }

    @Test
    @DisplayName("템플릿이 없으면 BusinessException(NOTIFICATION_TEMPLATE_NOT_FOUND)을 던진다")
    void getNotificationTemplateById_notFound() {
        // given
        Long templateId = 999L;
        when(notificationTemplateMapper.findById(templateId))
                .thenReturn(Optional.empty());

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.getNotificationTemplateById(templateId)
        );

        // then
        // ErrorCode 게터가 있다면 아래 검증을 사용하세요.
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);

        verify(notificationTemplateMapper, times(1)).findById(templateId);
        verifyNoMoreInteractions(notificationTemplateMapper);
    }
}
