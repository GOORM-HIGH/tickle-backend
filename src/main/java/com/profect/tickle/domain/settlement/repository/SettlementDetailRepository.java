package com.profect.tickle.domain.settlement.repository;

import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
}
