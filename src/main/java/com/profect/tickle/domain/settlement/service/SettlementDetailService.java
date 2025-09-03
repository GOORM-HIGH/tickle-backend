package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.batch.domain.settlement.mapper.SettlementDetailMapper;
import com.profect.tickle.batch.domain.settlement.timeUtil.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds.Settlement;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class SettlementDetailService {

    private final SettlementDetailMapper settlementDetailMapper;
    private final StatusProvider statusProvider;

    /**
     * 건별 정산 상태 업데이트
     * 업데이트 대상 <= n일 23시59분59초.999
     */
    @Transactional
    public void updateDetail() {
        // 00시00분에 스케줄 돌 때 23시59분59초 반환 메서드
        Instant endOfDay = SettlementTimeUtil.getEndOfDay();

        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

        // n일 00시00분에 스케줄 돌았을 때 n-1일 23시59분59초 이하의 공연들 중에 예매기간 끝난 공연 상태 upsert
        // 22일 23시59분59초 000에 예매종료인 공연이 있다면, 23일 00시00분에 스케줄러 돌고,
        // 22일 23시59분59초 999 이하인 모든 공연 중에서 정산 상태가 정산 예정인 공연을 정산 완료로 업데이트
        try {
            settlementDetailMapper.updateSettlementDetailStatus(beforeStatus, afterStatus, endOfDay);
        } catch (DataAccessException dae) {
            log.error("SettlementDetail update status 오류");
            // 에러 정보 상세 출력
            log.error("에러 메시지: {}", dae.getMessage());
            log.error("에러 원인: ", dae.getCause());
            log.error("스택 트레이스:", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_STATUS_UPDATE_FAILED);
        }
    }
}
