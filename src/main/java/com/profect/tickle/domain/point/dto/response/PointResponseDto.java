package com.profect.tickle.domain.point.dto.response;

import com.profect.tickle.domain.point.entity.Point;

public record PointResponseDto(
        String orderId,
        String orderName,
        String receiptId,
        int amount,
        int totalBalance,
        String username,
        String purchasedAt
) {
    public static PointResponseDto from(Point point,
                                        String receiptId,
                                        String orderName) {
        return new PointResponseDto(
                point.getOrderId(),
                orderName,
                receiptId,
                point.getCredit(),
                point.getMember().getPointBalance(),
                point.getMember().getNickname(),
                point.getCreatedAt().toString()
        );
    }
}
