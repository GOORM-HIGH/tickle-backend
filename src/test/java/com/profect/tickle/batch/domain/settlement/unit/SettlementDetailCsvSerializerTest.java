package com.profect.tickle.batch.domain.settlement.unit;

import com.profect.tickle.batch.domain.settlement.csvSerializer.SettlementCsvSerializer;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.global.status.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.profect.tickle.domain.member.entity.MemberRole.HOST;
import static org.assertj.core.api.Assertions.assertThat;

public class SettlementDetailCsvSerializerTest {

    private final SettlementCsvSerializer serializer = new SettlementCsvSerializer();

    @DisplayName("건별 정산의 CSV 직렬화 기능을 테스트한다.")
    @Test
    void settlementDetailCsvSerializerTest() {
        // Given
        SettlementDetailFindTargetDto dto = SettlementDetailFindTargetDto.of(
                1L, 9L, "정산 연산 테스트", Instant.parse("2025-08-31T00:00:00Z"),
                "202508300001", 100000L, new BigDecimal(0.01), Instant.parse("2025-08-31T00:00:00Z")
        );
        Member member = new Member(1L, "host1@email.com", "pwd1", "host1", null, "",
                HOST, "01012341234", 0, Instant.now(), Instant.now(), null,
                "", "", "", "", "",
                "", "", "", null, null);
        Status settlementStatus = Status.create(14L, "정산", (short) 100, "예매 예정", Instant.now());
        SettlementDetail item = SettlementDetail.create(dto, member, settlementStatus, 100000L,
                0L, 10000L, 1000L, 99000L, Instant.parse("2025-08-31T00:00:00Z"));

        // When
        String csv = serializer.detailCsvSerializer(List.of(item));

        // Then
        assertThat(csv).endsWith("\n");
        String[] cols = csv.split(",", -1);
        assertThat(cols[0]).isEqualTo(item.getMember().getId().toString());
        assertThat(cols[1]).isEqualTo(item.getStatus().getId().toString());
        assertThat(cols[2]).startsWith("\"").endsWith("\"");
        assertThat(cols[3]).isEqualTo("2025-08-31 00:00:00.000");
    }
}
