package com.profect.tickle.domain.contract.service;

import com.profect.tickle.domain.contract.entity.Contract;
import com.profect.tickle.domain.contract.repository.ContractRepository;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    @Transactional
    public void createContract(Member member, BigDecimal count) {

        Contract newContract = Contract.createContract(member, count);

        contractRepository.save(newContract);
    }

    @Transactional
    public void updateContract(Long memberId, BigDecimal newCharge) {

        Contract contract = contractRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND.getMessage(), ErrorCode.CONTRACT_NOT_FOUND));

        contract.updateCharge(newCharge);
    }
}
