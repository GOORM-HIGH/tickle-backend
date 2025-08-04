package com.profect.tickle.domain.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bootpay 결제 후 포인트 충전 요청 DTO")
public record ChargePointRequestDto(

        @Schema(description = "주문 ID", example = "order_1_20250803195541")
        String orderId,

        @Schema(description = "결제 수단 또는 주문 명", example = "Tickle 포인트 충전")
        String orderName,

        @Schema(description = "Bootpay에서 발급한 영수증 ID", example = "6ashhdf12as12839123")
        String receiptId,

        @Schema(description = "충전 금액", example = "1000")
        Integer amount,

        @Schema(description = "주문자 이름", example = "공연주최자")
        String username,

        @Schema(description = "결제 완료 시간 (ISO 형식 문자열)", example = "2025-08-03T19:55:57")
        String purchasedAt
) {}