package com.profect.tickle.domain.notification.dto.response;

import com.profect.tickle.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {

    private Long id;
    private String title;
    private String content;
    private boolean isRead;
    private Instant createdAt;

    /** Entity → DTO 변환 */
    public static NotificationResponseDto fromEntity(Notification entity) {
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .isRead(entity.getStatus().getDescription().equals("read"))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
