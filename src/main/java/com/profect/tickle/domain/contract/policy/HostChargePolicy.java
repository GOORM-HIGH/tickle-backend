package com.profect.tickle.domain.contract.policy;

public interface HostChargePolicy {

    boolean isAllowed(java.math.BigDecimal charge);

    String message(); // 에러 메시지 등에 표시할 안내
}
