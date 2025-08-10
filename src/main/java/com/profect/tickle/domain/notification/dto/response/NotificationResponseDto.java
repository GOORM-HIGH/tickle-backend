package com.profect.tickle.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponseDto {

    private Long id;
    private String title;
    private String content;
    private boolean isRead;
    private Instant createdAt;

    /** Entity → DTO 변환 */
//    public static NotificationResponseDto fromEntity(Notification entity) {
//        return NotificationResponseDto.builder()
//                .id(entity.getId())
//                .title(entity.getTitle())
//                .content(entity.getContent())
//                .isRead(entity.getStatus().getDescription().equals(""))
//                .createdAt(entity.getCreatedAt())
//                .build();
//    }
}
