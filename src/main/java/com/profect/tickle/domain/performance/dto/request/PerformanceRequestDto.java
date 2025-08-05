package com.profect.tickle.domain.performance.dto.request;

import com.profect.tickle.domain.performance.entity.HallType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Schema(description = "공연생성 요청 DTO")
public class PerformanceRequestDto {

    @Schema(description = "공연명", example = "흠뻑쇼")
    private String title;
    @Schema(description = "공연장르", example = "1")
    private Long genreId;
    @Schema(description = "공연날짜", example = "2025-04-13")
    private Instant date;
    @Schema(description = "공연상영시간", example = "120")
    private Short runtime;
    @Schema(description = "공연장 유형", example = "A")
    private HallType hallType;
    @Schema(description = "공연장소", example = "수원시 월드컵 경기장")
    private String hallAddress;
    @Schema(description = "예매시작날짜", example = "2025-03-05")
    private Instant startDate;
    @Schema(description = "예매종료날짜", example = "2025-03-10")
    private Instant endDate;
    @Schema(description = "이벤트참여여부", example = "false")
    private Boolean isEvent;
    @Schema(description = "이미지", example = "http://examplel.com")
    private String img;
}
