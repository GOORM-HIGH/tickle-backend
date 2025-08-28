package com.profect.tickle.domain.settlement.mapper;

import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "mybatis-test"})
@MybatisTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class SettlementDetailMapperTest {

    @Autowired
    private SettlementDetailMapper settlementDetailMapper;

    @DisplayName("조건에 맞는 예매 내역과 주최자의 수수료율을 조회한다.")
    @Test
    void findTargetReservations() {
        // Given
        Instant now = Instant.now();

        // When
        List<SettlementDetailFindTargetDto> targetReservations = settlementDetailMapper.findTargetReservations(now);

        // Then
        assertThat(targetReservations).hasSize(100000);
    }

}