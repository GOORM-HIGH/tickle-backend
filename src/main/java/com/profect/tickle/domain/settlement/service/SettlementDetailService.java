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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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

        // 건별정산 집계에 필요한 데이터
        List<SettlementDetailFindTargetDto> settlementTargets =
                Optional.ofNullable(settlementDetailMapper.findTargetReservations())
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_TARGET_DB_ERROR));

        if(settlementTargets.isEmpty()){
            log.info("정산 대상 데이터가 존재하지 않습니다.");
            return;
        }

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
                refundAmount = 0L;
                settlementStatus = statusProvider.provide(Settlement.SCHEDULED);
            } else if(reservationStatus.getId() == 10) {
                salesAmount = 0L;
                refundAmount = reservationPrice;
                settlementStatus = statusProvider.provide(Settlement.REFUND_REQUESTED);
            }

            Long grossAmount = salesAmount; // 정산대상금액 = 판매금액
            BigDecimal commission; // 수수료 초기화
            BigDecimal netAmount; // 대납금액 초기화
            try {
                // 수수료 = 판매금액 * 정산대상금액
                commission = contractCharge.multiply(BigDecimal.valueOf(grossAmount)).setScale(0, RoundingMode.HALF_UP);
                // 대납금액 = 정산대상금액 - 수수료
                netAmount = BigDecimal.valueOf(grossAmount).subtract(commission);
            } catch (NullPointerException | ArithmeticException e) {
                // → contractCharge 가 null 이었거나
                //   BigDecimal 연산 중 뭔가 비정상적인 상황이 생겼을 때
                log.error("수수료 계산 오류: {}, 대상 DTO={}", e.getMessage(), targetDto);
                throw new BusinessException(ErrorCode.SETTLEMENT_COMMISSION_CALCULATION_ERROR);
            }

            // dto에서 공연제목, 예매 종료일시, 예매코드, 적용 수수료율 추출
            SettlementDetail stlDetail = SettlementDetail.create(targetDto, member, settlementStatus,
                    salesAmount, refundAmount, grossAmount, commission.longValueExact(),
                    netAmount.longValueExact(), settlementCreatedAt);
            insertList.add(stlDetail);
        }
        // 마이바티스 foreach insert
        try {
            settlementDetailMapper.insertSettlementDetail(insertList);
        } catch (DataAccessException dae) {
            log.error("SettlementDetail insert 오류, List={}", insertList);
            throw new BusinessException(ErrorCode.SETTLEMENT_UPSERT_FAILED);
        }
    }

    /**
     * 건별 정산 상태 업데이트
     * 업데이트 대상 <= n일 23시59분59초.999
     */
    @Transactional
    public void updateDetail() {
        Instant endOfDay = SettlementTimeUtil.getEndOfDay();

        // 정산예정
        Status beforeStatus = statusProvider.provide(Settlement.SCHEDULED);
        // 정산완료
        Status afterStatus = statusProvider.provide(Settlement.COMPLETED);

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
