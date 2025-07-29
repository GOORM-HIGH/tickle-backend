package com.profect.tickle.domain.stl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "settlement_weekly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementWeekly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_weekly_id")
    private Long id;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "host_biz_name", length = 15, nullable = false)
    private String hostBizName;

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
}
