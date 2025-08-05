package com.profect.tickle.domain.settlement.entity;

import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "settlement_daily",
        uniqueConstraints = @UniqueConstraint(
                name = "uniq_settlement_daily",
                columnNames = {
                        "host_biz_name",
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
    @JoinColumn(name = "status_id", nullable = false)
    private Status statusId;

    @Column(name = "host_biz_name", length = 15, nullable = false)
    private String hostBizName;

    @Column(name = "performance_title", length = 50, nullable = false)
    private String performanceTitle;

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

    @Column(name = "settlement_daily_commission", nullable = false)
    private Long dailyCommission;

    @Column(name = "settlement_daily_net_amount", nullable = false)
    private Long dailyNetAmount;

    @Column(name = "settlement_daily_created_at", nullable = false)
    private LocalDateTime dailyCreatedAt;

}
