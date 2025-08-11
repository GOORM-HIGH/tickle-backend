package com.profect.tickle.domain.contract.repository;

import com.profect.tickle.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByMemberId(Long memberId);
}
