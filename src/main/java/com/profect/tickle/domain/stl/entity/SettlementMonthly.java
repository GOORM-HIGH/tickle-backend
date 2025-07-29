package com.profect.tickle.domain.stl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "settlement_monthly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_monthly_id")
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
}
