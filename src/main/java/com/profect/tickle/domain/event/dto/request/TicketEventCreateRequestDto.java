package com.profect.tickle.domain.event.dto.request;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "티켓 이벤트 생성 요청 DTO")
public record TicketEventCreateRequestDto(

        @NotNull(message = "공연 ID는 필수입니다.")
        @Schema(description = "공연 ID", example = "5")
        Long performanceId,

        @NotNull(message = "좌석 ID는 필수입니다.")
        @Schema(description = "좌석 ID", example = "5")
        Long seatId,

        @NotBlank(message = "이벤트명은 비어 있을 수 없습니다.")
        @Size(min = 2, message = "이벤트 이름은 2자 이상이어야 합니다.")
        @Schema(description = "이벤트명", example = "기프트 티켓 이벤트")
        String name,

        @NotNull(message = "목표 금액은 필수입니다.")
        @Min(value = 1000, message = "목표 금액은 최소 1000원 이상이어야 합니다.")
        @Schema(description = "목표 금액", example = "50000")
        Integer goalPrice,

        @NotNull(message = "응모 단위 금액은 필수입니다.")
        @Min(value = 100, message = "응모 단위 금액은 최소 100원 이상이어야 합니다.")
        @Schema(description = "응모 단위 금액", example = "200")
        Short perPrice
) {}