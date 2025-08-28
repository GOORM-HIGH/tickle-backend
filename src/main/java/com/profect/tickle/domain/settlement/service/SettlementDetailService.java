package com.profect.tickle.domain.settlement.service;

import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper;
import com.profect.tickle.domain.settlement.util.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds.Settlement;
import com.profect.tickle.global.status.repository.StatusRepository;
import com.profect.tickle.global.status.service.StatusProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SettlementDetailService {

    private final SettlementDetailMapper settlementDetailMapper;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;
    private final StatusProvider statusProvider;

    /**
     * 건별정산 연산 및 insert_tasklet 구조
     */
    public void getSettlementDetail(){
        // 정산 생성일시
        Instant settlementCreatedAt = Instant.now();

        int testCnt = settlementDetailMapper.findTargetReservationsCount(settlementCreatedAt);
        log.info("인스턴트 타입 현재 시간 개수: " + testCnt);

        // 읽기 시작
        Instant t0 = Instant.now();
        // 건별정산 집계에 필요한 데이터
        List<SettlementDetailFindTargetDto> settlementTargets =
                Optional.ofNullable(settlementDetailMapper.findTargetReservations(settlementCreatedAt))
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR));

        if(settlementTargets.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

        // 연산 시작
        Instant t1 = Instant.now();
        // 예매내역에서 집계된 데이터 금액 계산 후 주최자, 공연별로 리스트에 담기
        List<SettlementDetail> insertList = new ArrayList<>(); // 정산 결과 담을 리스트 생성
        for(SettlementDetailFindTargetDto targetDto : settlementTargets) {
            Member member = memberRepository.findById(targetDto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            Status reservationStatus = statusRepository.findById(targetDto.getReservationStatusId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

            Long reservationPrice = targetDto.getReservationPrice(); // 예매금액
            BigDecimal contractCharge = targetDto.getContractCharge(); // 적용 수수료율

            Long salesAmount = 0L; // 판매금액 초기화
            Long refundAmount = 0L; // 환불금액 초기화

            Status settlementStatus = null; // 정산상태 초기화(14=정산예정, 16=환불청구)
            if(reservationStatus.getId() == 9) {
                salesAmount = reservationPrice;
                settlementStatus = statusProvider.provide(Settlement.SCHEDULED);
            } else if(reservationStatus.getId() == 10) {
                refundAmount = reservationPrice;
                settlementStatus = statusProvider.provide(Settlement.REFUND_REQUESTED);
            }

            Long grossAmount = salesAmount; // 정산대상금액 = 판매금액
            // 수수료 = 판매금액 * 정산대상금액
            BigDecimal commission = contractCharge.multiply(BigDecimal.valueOf(grossAmount)).setScale(0, RoundingMode.HALF_UP);
            // 대납금액 = 정산대상금액 - 수수료
            BigDecimal netAmount = BigDecimal.valueOf(grossAmount).subtract(commission);

            // dto에서 공연제목, 예매 종료일시, 예매코드, 적용 수수료율 추출
            SettlementDetail stlDetail = SettlementDetail.create(targetDto, member, settlementStatus,
                    salesAmount, refundAmount, grossAmount, commission.longValueExact(),
                    netAmount.longValueExact(), settlementCreatedAt);
            insertList.add(stlDetail);
        }

        // 연산 완료
        Instant t2 = Instant.now();
        // 마이바티스 foreach insert
        // 연산된 데이터 db에 insert
        int insertSize = 5000;
        for(int i = 0; i < insertList.size(); i += insertSize){
            int toIndex = Math.min(i + insertSize, insertList.size());
            try {
                settlementDetailMapper.insertSettlementDetail(insertList.subList(i, toIndex));
            } catch (DataAccessException dae) {
                log.error("SettlementDetail insert 오류, List={}", insertList);
                throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
            }
        }
        // 저장 완료
        Instant t3 = Instant.now();

        log.info("읽기 걸린 시간 :: {} ms ", Duration.between(t0, t1).toMillis());
        log.info("연산 걸린 시간 :: {} ms", Duration.between(t1, t2).toMillis());
        log.info("저장 걸린 시간 :: {} ms", Duration.between(t2, t3).toMillis());
        log.info("총 걸린 시간 :: {} ms",Duration.between(t0, t3).toMillis());

    }

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
