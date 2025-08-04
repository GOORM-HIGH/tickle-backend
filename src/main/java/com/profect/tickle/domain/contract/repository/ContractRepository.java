package com.profect.tickle.domain.contract.repository;

import com.profect.tickle.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
}
