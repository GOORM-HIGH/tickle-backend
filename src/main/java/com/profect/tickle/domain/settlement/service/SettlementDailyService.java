package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDailyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDaily;
import com.profect.tickle.domain.settlement.mapper.SettlementDailyMapper;
import com.profect.tickle.domain.settlement.util.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementDailyService {

    private final SettlementDailyMapper settlementDailyMapper;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;

    /**
     * 일간정산 테이블 insert+update_tasklet 구조
     */
    public void getSettlementDaily() {
        // 정산 생성 시간
        Instant settlementDate = Instant.now();

        // 건별정산 데이터 집계 조회
        List<SettlementDailyFindTargetDto> aggregates =
                Optional.ofNullable(settlementDailyMapper.aggregateByDetail())
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR));

        if(aggregates.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        List<SettlementDaily> dailyList = new ArrayList<>(); // 정산 결과 담을 list 생성
        for(SettlementDailyFindTargetDto dto : aggregates) {
            Member member = memberRepository.findById(dto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Status settlementStatus = null; // 정산상태 초기화(14=정산예정, 15=정산완료)
            if(settlementDate.isBefore(dto.getPerformanceEndDate())) {
                settlementStatus = statusRepository.findById(14L)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
            } else if(settlementDate.isAfter(dto.getPerformanceEndDate())) {
                settlementStatus = statusRepository.findById(15L)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
            }

            SettlementDaily stlDaily = SettlementDaily.create(dto, member, settlementStatus, settlementDate);
            dailyList.add(stlDaily); // 결과 리스트에 담고
        }
        try {
            settlementDailyMapper.upsertSettlementDaily(dailyList); // upsert
        } catch (DataAccessException dae) {
            log.error("SettlementDaily upsert 오류, List={}", dailyList);
            throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
        }
    }

    /**
     * 1) 예매 종료일시 <= n일23시59분59초.999 && '정산예정'
     */
    @Transactional
    public void updateDailyByToday() {
        Instant endOfDay = SettlementTimeUtil.getEndOfDay();

        // 정산예정
        Status beforeStatus = statusRepository.findById(14L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
        // 정산완료
        Status afterStatus = statusRepository.findById(15L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        try {
            settlementDailyMapper.updateSettlementDailyStatus(beforeStatus, afterStatus, endOfDay);
        } catch (DataAccessException dae) {
            log.error("SettlementDaily update status by today 오류");
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
        Status beforeStatus = statusRepository.findById(14L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
        // 정산완료
        Status afterStatus = statusRepository.findById(15L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

        LocalDate now = SettlementTimeUtil.localDate(Instant.now());
        SettlementTimeUtil period = SettlementTimeUtil.get(now);
        int today = period.dayOfMonth();
        int monday = period.startOfWeek();

        if(today == 1 || today == monday) {
            try {
                settlementDailyMapper.updateSettlementDailyStatus(beforeStatus, afterStatus, endOfDay);
            } catch(DataAccessException dae) {
                log.error("SettlementDaily update status by boundary 오류");
                throw new BusinessException(ErrorCode.SETTLEMENT_STATUS_UPDATE_FAILED);
            }
        }
    }
}
