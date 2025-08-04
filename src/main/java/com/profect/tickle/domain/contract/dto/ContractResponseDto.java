package com.profect.tickle.domain.contract.dto;

import com.profect.tickle.domain.contract.entity.Contract;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class ContractResponseDto {

    private Long contractId;           // 계약 고유번호
    private Long userId;               // 회원 고유번호
    private BigDecimal contractCharge; // 계약 수수료
    private Instant contractCreatedAt; // 계약 생성일

    // Entity → DTO 변환
    public static ContractResponseDto fromEntity(Contract entity) {
        return ContractResponseDto.builder()
                .contractId(entity.getContractId())
                .userId(entity.getMember().getId())
                .contractCharge(entity.getCharge())
                .contractCreatedAt(entity.getCreatedAt())
                .build();
    }
}
