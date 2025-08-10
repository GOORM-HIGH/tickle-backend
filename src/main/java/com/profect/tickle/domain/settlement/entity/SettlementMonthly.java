package com.profect.tickle.domain.settlement.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.settlement.dto.batch.SettlementMonthlyFindTargetDto;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "settlement_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_settlement_monthly",
                columnNames = {
                        "member_id",
                        "performance_title",
                        "settlement_year",
                        "settlement_month"
                }
        ))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettlementMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_monthly_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "performance_title", length = 50, nullable = false)
    private String performanceTitle;

    @Column(name = "settlement_year", length = 4, nullable = false)
    private String year;

    @Column(name = "settlement_month", length = 2, nullable = false)
    private String month;

    @Column(name = "settlement_monthly_sales_amount", nullable = false)
    private Long monthlySalesAmount;

    @Column(name = "settlement_monthly_refund_amount", nullable = false)
    private Long monthlyRefundAmount;

    @Column(name = "settlement_monthly_gross_amount", nullable = false)
    private Long monthlyGrossAmount;

    @Column(name = "settlement_monthly_commission", nullable = false)
    private Long monthlyCommission;

    @Column(name = "settlement_monthly_net_amount", nullable = false)
    private Long monthlyNetAmount;

    @Column(name = "settlement_monthly_created_at", nullable = false)
    private Instant monthlyCreatedAt;

    @Column(name = "settlement_monthly_updated_at")
    private Instant monthlyUpdatedAt;

    public static SettlementMonthly create(SettlementMonthlyFindTargetDto dto,
                                           Member member,
                                           Status status,
                                           String year,
                                           String month,
                                           Instant now) {
        return SettlementMonthly.builder()
                .member(member)
                .status(status)
                .performanceTitle(dto.getPerformanceTitle())
                .year(year)
                .month(month)
                .monthlySalesAmount(dto.getMonthlySalesAmount())
                .monthlyRefundAmount(dto.getMonthlyRefundAmount())
                .monthlyGrossAmount(dto.getMonthlyGrossAmount())
                .monthlyCommission(dto.getMonthlyCommission())
                .monthlyNetAmount(dto.getMonthlyNetAmount())
                .monthlyCreatedAt(now)
                .build();
    }

}
