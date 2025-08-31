package com.profect.tickle.batch.domain.settlement.unit;

import com.profect.tickle.batch.domain.settlement.DetailBatchConfig;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.repository.StatusRepository;
import com.profect.tickle.global.status.service.StatusProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static com.profect.tickle.domain.member.entity.MemberRole.HOST;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SettlementDetailProcessorTest {

    @InjectMocks
    private DetailBatchConfig detailBatchConfig;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private StatusProvider statusProvider;

    @DisplayName("예매 내역이 결제 상태일 때 정산 금액이 정상적으로 계산된다.")
    @Test
    void settlementDetailProcessorWhenReservationStatusIs9() throws Exception {
        // Given
        SettlementDetailFindTargetDto dto = SettlementDetailFindTargetDto.of(
                1L, 9L, "정산 연산 테스트", Instant.parse("2025-08-31T00:00:00Z"),
                "202508300001", 100000L, new BigDecimal(0.01), Instant.parse("2025-08-31T00:00:00Z")
        );

        Status reservationStatus = Status.create(9L, "예매", (short) 102, "예매 결제", Instant.now());
        Status settlementStatus = Status.create(14L, "정산", (short) 100, "예매 예정", Instant.now());
        BDDMockito.given(memberRepository.findById(1L))
                .willReturn(Optional.of(new Member(
                        1L, "host1@email.com", "pwd1", "host1", null, "", HOST, "01012341234",
                        0, Instant.now(), Instant.now(), null, "", "", "", "", "", "", "", "", null, null
                )));
        BDDMockito.given(statusRepository.findById(9L))
                .willReturn(Optional.of(reservationStatus));
        BDDMockito.given(statusProvider.provide(StatusIds.Settlement.SCHEDULED))
                .willReturn(settlementStatus);

        // When
        ItemProcessor<SettlementDetailFindTargetDto, SettlementDetail> processor
                = detailBatchConfig.settlementDetailProcessor();

        SettlementDetail result = processor.process(dto);

        // Then
        assertThat(result.getStatus()).isEqualTo(settlementStatus);
        assertThat(result.getSalesAmount()).isEqualTo(100000L);
        assertThat(result.getRefundAmount()).isEqualTo(0L);
        assertThat(result.getGrossAmount()).isEqualTo(100000L);
        assertThat(result.getCommission()).isEqualTo(1000L);
        assertThat(result.getNetAmount()).isEqualTo(99000L);
    }

    @DisplayName("예매 내역이 취소 상태일 때 환불 금액이 정상적으로 계산된다.")
    @Test
    void settlementDetailProcessorWhenReservationStatusIs10() throws Exception {
        // Given
        SettlementDetailFindTargetDto dto = SettlementDetailFindTargetDto.of(
                1L, 10L, "정산 연산 테스트", Instant.parse("2025-08-31T00:00:00Z"),
                "202508300001", 100000L, new BigDecimal(0.01), Instant.parse("2025-08-31T00:00:00Z")
        );

        Status reservationStatus = Status.create(10L, "예매", (short) 103, "예매 취소", Instant.now());
        Status settlementStatus = Status.create(16L, "정산", (short) 103, "환불 청구", Instant.now());
        BDDMockito.given(memberRepository.findById(1L))
                .willReturn(Optional.of(new Member(
                        1L, "host1@email.com", "pwd1", "host1", null, "", HOST, "01012341234",
                        0, Instant.now(), Instant.now(), null, "", "", "", "", "", "", "", "", null, null
                )));
        BDDMockito.given(statusRepository.findById(10L))
                .willReturn(Optional.of(reservationStatus));
        BDDMockito.given(statusProvider.provide(StatusIds.Settlement.REFUND_REQUESTED))
                .willReturn(settlementStatus);

        // When
        ItemProcessor<SettlementDetailFindTargetDto, SettlementDetail> processor
                = detailBatchConfig.settlementDetailProcessor();

        SettlementDetail result = processor.process(dto);

        // Then
        assertThat(result.getStatus()).isEqualTo(settlementStatus);
        assertThat(result.getSalesAmount()).isEqualTo(0L);
        assertThat(result.getRefundAmount()).isEqualTo(100000L);
        assertThat(result.getGrossAmount()).isEqualTo(0L);
        assertThat(result.getCommission()).isEqualTo(0L);
        assertThat(result.getNetAmount()).isEqualTo(0L);
    }
}
