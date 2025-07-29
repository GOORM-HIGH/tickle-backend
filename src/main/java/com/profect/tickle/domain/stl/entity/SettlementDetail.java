package com.profect.tickle.domain.stl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "settlement_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_detail_id")
    private Long id;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "host_biz_name", length = 15, nullable = false)
    private String hostBizName;

    @Column(name = "performance_title", length = 50, nullable = false)
    private String performanceTitle;

    @Column(name = "performance_end_date", nullable = false)
    private LocalDateTime performanceEndDate;

    @Column(name = "settlement_detail_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "settlement_detail_sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "settlement_detail_refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "settlement_detail_gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name = "settlement_detail_commission", nullable = false)
    private Long commission;

    @Column(name = "settlement_detail_net_amount", nullable = false)
    private Long netAmount;
}
