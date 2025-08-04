package com.profect.tickle.domain.performance.dto.response;

import com.profect.tickle.domain.performance.entity.Performance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceResponseDto {
    private Long performanceId; //공연고유번호
    private String status; //공연 상태
    private String title; //공연명
    private String genreTitle; //공연장르
    private String price; //공연가격
    private LocalDateTime date; //공연날짜
    private Short runtime; //공연상영시간
    private String hallType; //공연유형
    private String hallAddress; //공연장소
    private String hostBizName; //주최측
    private LocalDateTime startDate; //예매시작날짜
    private LocalDateTime endDate; //예매종료날짜
    private Boolean isEvent; //이벤트참여여부
    private String img; //이미지
    private LocalDateTime createdAt; //생성시간
    private LocalDateTime updatedAt; //업데이트 시간

    public static PerformanceResponseDto from(Performance performance) {
        return PerformanceResponseDto.builder()
                .performanceId(performance.getId())
                .status(performance.getStatus().getDescription())
                .title(performance.getTitle())
                .genreTitle(performance.getGenre().getTitle())
                .price(performance.getPrice())
                .runtime(performance.getRuntime())
                .hostBizName(performance.getMember().getHostBizName())
                .hallType(performance.getHall().getType().toString())
                .hallAddress(performance.getHall().getAddress())
                .startDate(performance.getStartDate())
                .endDate(performance.getEndDate())
                .date(performance.getDate())
                .img(performance.getImg())
                .isEvent(performance.getIsEvent())
                .createdAt(performance.getCreatedAt())
                .updatedAt(performance.getUpdatedAt())
                .build();
    }

}
