package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.batch.domain.settlement.mapper.SettlementDailyMapper;
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
public class SettlementDailyService {

    private final SettlementDailyMapper settlementDailyMapper;
    private final StatusProvider statusProvider;

    /**
     * 1) 예매 종료일시 <= n일23시59분59초.999 && '정산예정'
     */
    @Transactional
    public void updateDailyByToday() {
        Instant endOfDay = SettlementTimeUtil.getEndOfDay();

        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

        try {
            settlementDailyMapper.updateSettlementDailyStatus(beforeStatus, afterStatus, endOfDay);
        } catch (DataAccessException dae) {
            log.error("SettlementDaily update status by today 오류");
            // 에러 정보 상세 출력
            log.error("에러 메시지: {}", dae.getMessage());
            log.error("에러 원인: ", dae.getCause());
            log.error("스택 트레이스:", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_STATUS_UPDATE_FAILED);
        }
    }

    /**
     * 2) '오늘이 월요일 또는 1일' && '정산예정'이라면 어제까지의 일별 정산 건들 업데이트
     */
    @Transactional
    public void updateDailyByBoundary() {
        Instant endOfDay = SettlementTimeUtil.getEndOfDay();

        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

        LocalDate now = SettlementTimeUtil.localDate(Instant.now());
        SettlementTimeUtil period = SettlementTimeUtil.get(now);
        int today = period.dayOfMonth();
        int monday = period.startOfWeek();

        if(today == 1 || today == monday) {
            try {
                settlementDailyMapper.updateSettlementDailyStatus(beforeStatus, afterStatus, endOfDay);
            } catch(DataAccessException dae) {
                log.error("SettlementDaily update status by boundary 오류");
                // 에러 정보 상세 출력
                log.error("에러 메시지: {}", dae.getMessage());
                log.error("에러 원인: ", dae.getCause());
                log.error("스택 트레이스:", dae);
                throw new BusinessException(ErrorCode.SETTLEMENT_STATUS_UPDATE_FAILED);
            }
        }
    }
}
