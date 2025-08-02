package com.profect.tickle.domain.point.dto;

import com.profect.tickle.domain.point.entity.Point;
import com.profect.tickle.domain.point.entity.PointTarget;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class PointResponseDTO {

    private Long id;            // 포인트 고유번호
    private Long userId;             // 사용자 고유번호
    private Integer credit;     // 충전/차감 포인트
    private PointTarget target;      // 사용 대상
    private Integer result;     // 포인트 잔액 결과
    private Instant createdAt;  // 생성일시

    /** Entity → DTO 변환 */
    public static PointResponseDTO fromEntity(Point pointEntity) {
        return PointResponseDTO.builder()
                .id(pointEntity.getPointId())
                .userId(pointEntity.getMember().getId())
                .credit(pointEntity.getCredit())
                .target(pointEntity.getTarget())
                .result(pointEntity.getResult())
                .createdAt(pointEntity.getCreatedAt())
                .build();
    }
}
