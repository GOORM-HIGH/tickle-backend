package com.profect.tickle.domain.point.dto.response;

import com.profect.tickle.domain.point.entity.Point;

public record PointSimpleResponseDto(
        int credit,
        String target,
        String orderId,
        String createdAt
) {
    public static PointSimpleResponseDto from(Point point) {
        return new PointSimpleResponseDto(
                point.getCredit(),
                point.getTarget().name(),
                point.getOrderId(),
                point.getCreatedAt().toString()
        );
    }
    public static PointSimpleResponseDto from(int totalBalance) {
        return new PointSimpleResponseDto(totalBalance, "BALANCE", "-", "-");
    }
}