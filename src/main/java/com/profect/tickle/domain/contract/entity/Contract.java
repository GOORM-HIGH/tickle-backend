package com.profect.tickle.domain.contract.entity;

import com.profect.tickle.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private Short contractCharge;  // 계약 수수료 (1~10%)

    @Column(name = "contract_created_at", nullable = false, updatable = false)
    private LocalDateTime contractCreatedAt;  // 계약 생성일

    @PrePersist
    public void prePersist() {
        this.contractCreatedAt = LocalDateTime.now();
    }
}