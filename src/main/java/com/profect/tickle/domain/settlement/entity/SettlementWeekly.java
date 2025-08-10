package com.profect.tickle.domain.settlement.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyFindTargetDto;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "settlement_weekly",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_settlement_weekly",
                columnNames = {
                        "member_id",
                        "performance_title",
                        "settlement_year",
                        "settlement_month",
                        "settlement_week"
                }
        ))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettlementWeekly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_weekly_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id" , nullable = false)
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

    @Column(name = "settlement_week", length = 2, nullable = false)
    private String week;

    @Column(name = "settlement_weekly_sales_amount", nullable = false)
    private Long weeklySalesAmount;

    @Column(name = "settlement_weekly_refund_amount", nullable = false)
    private Long weeklyRefundAmount;

    @Column(name = "settlement_weekly_gross_amount", nullable = false)
    private Long weeklyGrossAmount;

    @Column(name = "settlement_weekly_commission", nullable = false)
    private Long weeklyCommission;

    @Column(name = "settlement_weekly_net_amount", nullable = false)
    private Long weeklyNetAmount;

    @Column(name = "settlement_weekly_created_at", nullable = false)
    private Instant weeklyCreatedAt;

    @Column(name = "settlement_weekly_updated_at")
    private Instant weeklyUpdatedAt;

    public static SettlementWeekly create(SettlementWeeklyFindTargetDto dto,
                                          Member member,
                                          Status status,
                                          String year,
                                          String month,
                                          String week,
                                          Instant now) {
        return SettlementWeekly.builder()
                .member(member)
                .status(status)
                .performanceTitle(dto.getPerformanceTitle())
                .year(year)
                .month(month)
                .week(week)
                .weeklySalesAmount(dto.getWeeklySalesAmount())
                .weeklyRefundAmount(dto.getWeeklyRefundAmount())
                .weeklyGrossAmount(dto.getWeeklyGrossAmount())
                .weeklyCommission(dto.getWeeklyCommission())
                .weeklyNetAmount(dto.getWeeklyNetAmount())
                .weeklyCreatedAt(now)
                .build();
    }
}
