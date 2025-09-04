package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.batch.domain.settlement.mapper.SettlementMonthlyMapper;
import com.profect.tickle.batch.domain.settlement.timeUtil.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds.Settlement;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementMonthlyService {

    private final SettlementMonthlyMapper settlementMonthlyMapper;
    private final StatusProvider statusProvider;

    /**
     * 오늘이 1일이면 지난 달 정산 내역 업데이트
     */
    @Transactional
    public void updateMonthly() {
        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

        LocalDate now = SettlementTimeUtil.localDate(Instant.now());
        SettlementTimeUtil period = SettlementTimeUtil.get(now);
        int today = period.dayOfMonth();
        String year = period.yearStr();
        String month = String.format("%02d", period.month()-1);

        if(today == 1) {
            try {
                settlementMonthlyMapper.updateSettlementMonthlyStatus(beforeStatus, afterStatus, year, month);
            } catch (DataAccessException dae) {
                log.error("SettlementMonthly update status 오류");
                // 에러 정보 상세 출력
                log.error("에러 메시지: {}", dae.getMessage());
                log.error("에러 원인: ", dae.getCause());
                log.error("스택 트레이스: ", dae);
                throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
            }
        }
    }
}
