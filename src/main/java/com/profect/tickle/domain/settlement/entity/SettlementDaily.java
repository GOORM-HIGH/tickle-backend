package com.profect.tickle.domain.settlement.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyFindTargetDto;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(
        name = "settlement_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_settlement_daily",
                columnNames = {
                        "member_id",
                        "performance_title",
                        "settlement_year",
                        "settlement_month",
                        "settlement_day"
                }
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettlementDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_daily_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "performance_title", length = 50, nullable = false)
    private String performanceTitle;

    @Column(name = "performance_end_date", nullable = false)
    private Instant performanceEndDate;

    @Column(name = "settlement_year", length = 4, nullable = false)
    private String year;

    @Column(name = "settlement_month", length = 2, nullable = false)
    private String month;

    @Column(name = "settlement_day", length = 2, nullable = false)
    private String day;

    @Column(name = "settlement_daily_sales_amount", nullable = false)
    private Long dailySalesAmount;

    @Column(name = "settlement_daily_refund_amount", nullable = false)
    private Long dailyRefundAmount;

    @Column(name = "settlement_daily_gross_amount", nullable = false)
    private Long dailyGrossAmount;

    @Column(name ="contract_charge",
            precision = 38,
            scale = 2,
            nullable = false)
    private BigDecimal contractCharge;

    @Column(name = "settlement_daily_commission", nullable = false)
    private Long dailyCommission;

    @Column(name = "settlement_daily_net_amount", nullable = false)
    private Long dailyNetAmount;

    @Column(name = "settlement_daily_created_at", nullable = false)
    private Instant dailyCreatedAt;

    @Column(name = "settlement_daily_updated_at")
    private Instant dailyUpdatedAt;

    public static SettlementDaily create(SettlementDailyFindTargetDto dto,
                                         Member member,
                                         Status status,
                                         Instant now) {
        return SettlementDaily.builder()
                .member(member)
                .status(status)
                .performanceTitle(dto.getPerformanceTitle())
                .performanceEndDate(dto.getPerformanceEndDate())
                .year(dto.getYear())
                .month(dto.getMonth())
                .day(dto.getDay())
                .dailySalesAmount(dto.getDailySalesAmount())
                .dailyRefundAmount(dto.getDailyRefundAmount())
                .dailyGrossAmount(dto.getDailyGrossAmount())
                .contractCharge(dto.getContractCharge())
                .dailyCommission(dto.getDailyCommission())
                .dailyNetAmount(dto.getDailyNetAmount())
                .dailyCreatedAt(now)
                .build();
    }
}
