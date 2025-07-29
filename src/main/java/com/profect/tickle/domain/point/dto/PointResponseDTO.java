package com.profect.tickle.domain.point.dto;

import com.profect.tickle.domain.point.entity.Point;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointResponseDTO {

    private Long pointId;            // 포인트 고유번호
    private Long userId;             // 사용자 고유번호
    private Integer pointCredit;     // 충전/차감 포인트
    private String pointTarget;      // 사용 대상
    private Integer pointResult;     // 포인트 잔액 결과
    private LocalDateTime pointCreatedAt;  // 생성일시

    /** Entity → DTO 변환 */
    public static PointResponseDTO fromEntity(Point pointEntity) {
        return PointResponseDTO.builder()
                .pointId(pointEntity.getPointId())
                .userId(pointEntity.getUserId())
                .pointCredit(pointEntity.getPointCredit())
                .pointTarget(pointEntity.getPointTarget())
                .pointResult(pointEntity.getPointResult())
                .pointCreatedAt(pointEntity.getPointCreatedAt())
                .build();
    }
}
