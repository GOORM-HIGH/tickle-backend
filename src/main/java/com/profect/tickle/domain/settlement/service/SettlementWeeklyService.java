package com.profect.tickle.domain.settlement.service;

import static com.profect.tickle.global.status.StatusIds.*;

import com.profect.tickle.batch.domain.settlement.mapper.SettlementWeeklyMapper;
import com.profect.tickle.batch.domain.settlement.timeUtil.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementWeeklyService {

    private final SettlementWeeklyMapper settlementWeeklyMapper;
    private final StatusProvider statusProvider;

    /**
     * '오늘이 월요일 또는 1일' && '정산예정'인 n-1회차 건들 업데이트
     */
    @Transactional
    public void updateWeekly() {
        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

        LocalDate now = SettlementTimeUtil.localDate(Instant.now());
        SettlementTimeUtil period = SettlementTimeUtil.get(now);
        int today = period.dayOfMonth();
        int monday = period.startOfWeek();
        String year = period.yearStr();
        String month = period.monthStr();
        String week = String.format("%02d", period.weekOfMonth()-1);

        if(today == 1 || today == monday) {
            try {
                settlementWeeklyMapper.updateSettlementDetailStatus(
                        beforeStatus, afterStatus,year, month, week);
            } catch(DataAccessException dae) {
                log.error("SettlementWeekly update status 오류");
                // 에러 정보 상세 출력
                log.error("에러 메시지: {}", dae.getMessage());
                log.error("에러 원인: ", dae.getCause());
                log.error("스택 트레이스:", dae);
                throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
            }
        }
    }
}
