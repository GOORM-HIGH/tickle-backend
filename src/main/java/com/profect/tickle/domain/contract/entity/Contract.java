package com.profect.tickle.domain.contract.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "Contract")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;  // 계약 고유번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "contract_charge", nullable = false)
    private BigDecimal charge;  // 계약 수수료

    @Column(name = "contract_created_at", nullable = false, updatable = false)
    private Instant createdAt;  // 계약 생성일

    public static Contract createContract(Member newMember, BigDecimal hostContractCharge) {
        return Contract.builder()
                .member(newMember)
                .charge(hostContractCharge)
                .createdAt(newMember.getCreatedAt())
                .build();
    }
}