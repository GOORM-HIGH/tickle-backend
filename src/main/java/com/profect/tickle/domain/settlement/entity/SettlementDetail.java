package com.profect.tickle.domain.settlement.entity;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.global.status.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "settlement_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_detail_id")
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

    @Column(name = "reservation_code", length = 15, nullable = false)
    private String reservationCode;

    @Column(name = "settlement_detail_sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "settlement_detail_refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "settlement_detail_gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name ="contract_charge",
            precision = 38,
            scale = 2,
            nullable = false)
    private BigDecimal contractCharge;

    @Column(name = "settlement_detail_commission", nullable = false)
    private Long commission;

    @Column(name = "settlement_detail_net_amount", nullable = false)
    private Long netAmount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "settlement_detail_created_at", nullable = false)
    private Instant createdAt;

    public static SettlementDetail create(SettlementDetailFindTargetDto dto,
                                          Member member,
                                          Status status,
                                          Long salesAmount,
                                          Long refundAmount,
                                          Long grossAmount,
                                          Long commission,
                                          Long netAmount,
                                          Instant now) {
        return SettlementDetail.builder()
                .member(member)
                .status(status)
                .performanceTitle(dto.getPerformanceTitle())
                .performanceEndDate(dto.getPerformanceEndDate())
                .reservationCode(dto.getReservationCode())
                .salesAmount(salesAmount)
                .refundAmount(refundAmount)
                .grossAmount(grossAmount)
                .contractCharge(dto.getContractCharge())
                .commission(commission)
                .netAmount(netAmount)
                .createdAt(now)
                .build();
    }
}
