package com.profect.tickle.domain.contract.dto;

import com.profect.tickle.domain.contract.entity.Contract;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContractResponseDTO {

    private Long contractId;           // 계약 고유번호
    private Long userId;               // 회원 고유번호
    private Short contractCharge;      // 계약 수수료
    private LocalDateTime contractCreatedAt; // 계약 생성일

    /** Entity → DTO 변환 */
    public static ContractResponseDTO fromEntity(Contract contractEntity) {
        return ContractResponseDTO.builder()
                .contractId(contractEntity.getContractId())
                .userId(contractEntity.getUserId())
                .contractCharge(contractEntity.getContractCharge())
                .contractCreatedAt(contractEntity.getContractCreatedAt())
                .build();
    }
}
