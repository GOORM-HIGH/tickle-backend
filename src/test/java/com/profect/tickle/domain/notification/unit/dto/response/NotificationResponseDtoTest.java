package com.profect.tickle.domain.notification.unit.dto.response;

import com.profect.tickle.domain.notification.dto.response.NotificationResponseDto;
import com.profect.tickle.domain.notification.dto.response.NotificationSseResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationResponseDtoTest {

    @Test
    @DisplayName("NotificationSseResponseDto 빌더로 생성된 값이 정상적으로 매핑된다")
    void notificationSseResponseDto_builderAndGetter() {
        // given
        String title = "테스트 알림";
        String message = "알림 내용입니다.";

        // when
        NotificationSseResponseDto dto = NotificationSseResponseDto.builder()
                .title(title)
                .message(message)
                .build();

        // then
        assertNotNull(dto);
        assertEquals(title, dto.getTitle());
        assertEquals(message, dto.getMessage());
    }

    @Test
    @DisplayName("NotificationResponseDto 빌더로 생성된 값이 정상적으로 매핑된다")
    void notificationResponseDto_builderAndGetter() {
        // given
        Long id = 100L;
        String title = "공지사항";
        String content = "서비스 점검 안내";
        boolean isRead = true;
        Instant createdAt = Instant.now();

        // when
        NotificationResponseDto dto = NotificationResponseDto.builder()
                .id(id)
                .title(title)
                .content(content)
                .isRead(isRead)
                .createdAt(createdAt)
                .build();

        // then
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals(title, dto.getTitle());
        assertEquals(content, dto.getContent());
        assertTrue(dto.isRead());
        assertEquals(createdAt, dto.getCreatedAt());
    }
}
