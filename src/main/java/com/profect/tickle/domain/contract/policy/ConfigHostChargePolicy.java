package com.profect.tickle.domain.contract.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConfigHostChargePolicy implements HostChargePolicy {

    private final com.profect.tickle.domain.contract.config.ContractSettings settings;

    private BigDecimal norm(BigDecimal v) {
        // 소수점 둘째 자리까지 고정(0.01 단위)로 비교
        return v.setScale(2, RoundingMode.UNNECESSARY);
    }

    private Set<BigDecimal> allowed() {
        return settings.getAllowedChargeList().stream()
                .map(this::norm)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isAllowed(BigDecimal charge) {
        if (charge == null) return false;
        return allowed().contains(norm(charge));
    }

    @Override
    public String message() {
        return "허용 수수료율: " + settings.getAllowedChargeList();
    }
}