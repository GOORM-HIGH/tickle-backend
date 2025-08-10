package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementMonthlyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementMonthly;
import com.profect.tickle.domain.settlement.mapper.SettlementMonthlyMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementMonthlyService {

    private final SettlementMonthlyMapper settlementMonthlyMapper;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;

    public void getSettlementMonthly(){
        HashMap<String, Object> map = new HashMap<>();

        // 정산 생성 시간
        Instant settlementDate = Instant.now();

        // 날짜 유틸 yyyy, m, week
        // 00시00분30초에 어제 날짜 기준으로 해당 주차에 포함되는 주간 정산 데이터 집계
        LocalDate today = LocalDate.now();
        SettlementTimeUtil period = SettlementTimeUtil.get(today);
        map.put("year", period.yearStr());
        map.put("month", period.monthStr());
        map.put("now", settlementDate);

        // 월간에 upsert할 주간 정산 조회
        List<SettlementMonthlyFindTargetDto> aggregates =
                Optional.ofNullable(settlementMonthlyMapper.findByWeek(map))
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR));

        if(aggregates.isEmpty()){
            log.error("정산 대상 데이터가 존재하지 않습니다.");
        }

        // 월간에 upsert
        List<SettlementMonthly> monthlyList = new ArrayList<>();
        for(SettlementMonthlyFindTargetDto dto : aggregates){
            Member member = memberRepository.findById(dto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            Status settlementStatus = statusRepository.findById(14L)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

            SettlementMonthly stlMonthly = SettlementMonthly.create(dto, member, settlementStatus,
                    period.yearStr(), period.monthStr(), settlementDate);
            monthlyList.add(stlMonthly);
        }
        try {
            settlementMonthlyMapper.upsertSettlementMonthly(monthlyList);
        } catch (DataAccessException dae) {
            log.error("SettlementMonthly upsert 오류, list={}", monthlyList);
            throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
        }
    }

    /**
     * 오늘이 1일이면 지난 달 정산 내역 업데이트
     */
    @Transactional
    public void updateMonthly() {
        // 정산예정
        Status beforeStatus = statusRepository.findById(14L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));
        // 정산완료
        Status afterStatus = statusRepository.findById(15L)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

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
                throw new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR);
            }
        }
    }
}
