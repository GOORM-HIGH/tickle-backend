package com.profect.tickle.domain.settlement.service;

import static com.profect.tickle.global.status.StatusIds.*;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementWeeklyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementWeekly;
import com.profect.tickle.domain.settlement.mapper.SettlementWeeklyMapper;
import com.profect.tickle.domain.settlement.util.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.repository.StatusRepository;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementWeeklyService {

    private final SettlementWeeklyMapper settlementWeeklyMapper;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;
    private final StatusProvider statusProvider;

    /**
     * 주간정산 테이블 insert+update_tasklet 구조
     */
    public void getSettlementWeekly(){
        HashMap<String, Object> map = new HashMap<>();

        // 정산 생성 시간
        Instant settlementDate = Instant.now();

        // 날짜 유틸 yyyy, m, week
        // 10분마다 당일의 정산내역 집계 (yesterday)
        LocalDate today = SettlementTimeUtil.localDate(settlementDate);
        SettlementTimeUtil period = SettlementTimeUtil.get(today);
        map.put("year", period.yearStr());
        map.put("month", period.monthStr());
        map.put("day", period.dayOfMonthStr());
        map.put("week", period.weekOfMonthStr());
        map.put("from", period.yearStr() + "-" + period.monthStr() + "-" + period.startOfWeek());
        map.put("to", period.yearStr() + "-" + period.monthStr() + "-" + period.endOfWeek());
        map.put("now", settlementDate);

        // 오늘 날짜 기준 해당 주차의 일간정산 합계 추출
        List<SettlementWeeklyFindTargetDto> aggregates =
                Optional.ofNullable(settlementWeeklyMapper.findByDate(map))
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR));

        if(aggregates.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        // 주최자, 공연, 회차별로 upsert
        List<SettlementWeekly> weeklyList = new ArrayList<>();
        for(SettlementWeeklyFindTargetDto dto : aggregates) {
            Member member = memberRepository.findById(dto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            Status settlementStatus = statusProvider.provide(Settlement.SCHEDULED);

            SettlementWeekly stlWeekly = SettlementWeekly.create(dto, member, settlementStatus,
                    period.yearStr(), period.monthStr(), period.weekOfMonthStr(), settlementDate);

            weeklyList.add(stlWeekly);
        }
        // 날짜 정보, 일간 dto로 주간 테이블에 upsert
        try {
            settlementWeeklyMapper.upsertSettlementWeekly(weeklyList);
        } catch (DataAccessException dae) {
            log.error("SettlementWeekly upsert 오류, list={}", weeklyList);
            // 에러 정보 상세 출력
            log.error("에러 메시지: {}", dae.getMessage());
            log.error("에러 원인: ", dae.getCause());
            log.error("스택 트레이스:", dae);
            throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
        }
    }

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
