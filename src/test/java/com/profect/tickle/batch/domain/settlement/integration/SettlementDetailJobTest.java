package com.profect.tickle.batch.domain.settlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("mybatis-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "DELETE FROM settlement_detail")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = "DELETE FROM batch_metadata")
public class SettlementDetailJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 로컬DB로 테스트를 진행해서 이후에 진행 시에는 더미 데이터와 날짜가 맞지 않아 테스트가 통과하지 않을 수 있습니다..
     */
    @DisplayName("건별 정산 배치 Job이 정상적으로 실행되는지 테스트한다.")
    @Test
    void settlementDetailJobWriterTest() throws Exception {
        // Given
        JobParameters params = new JobParametersBuilder()
                .addString("settlementDetailCreatedAt", Instant.now().toString())
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // When

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_detail", Integer.class);
        Integer sumSalesAmount = jdbcTemplate.queryForObject(
                "SELECT SUM(settlement_detail_sales_amount) FROM settlement_detail", Integer.class);
        assertThat(cnt).isEqualTo(4);
        assertThat(sumSalesAmount).isEqualTo(400000);
    }
}
