package com.profect.tickle.domain.notification.dto.response;

import com.profect.tickle.domain.notification.entity.Notification;
import com.profect.tickle.global.status.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDTO {

    private Long id;
    private String title;
    private String content;
    private Status status;
    private LocalDateTime createdAt;

    /** Entity → DTO 변환 */
    public static NotificationResponseDTO fromEntity(Notification entity) {
        return NotificationResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
