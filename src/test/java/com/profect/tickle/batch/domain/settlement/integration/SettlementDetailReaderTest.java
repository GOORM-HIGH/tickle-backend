package com.profect.tickle.batch.domain.settlement.integration;

import com.profect.tickle.batch.domain.settlement.DetailBatchConfig;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("mybatis-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SettlementDetailReaderTest {

    @Autowired
    private DetailBatchConfig detailBatchConfig;
    @Autowired
    ApplicationContext context;

    @DisplayName("조건에 맞는 예매 내역을 건별 정산 대상으로 불러온다.")
    @Test
    void settlementDetailReaderTest() throws Exception {
        // Given
        JobParameters params = new JobParametersBuilder()
                .addString("settlementDetailCreatedAt", Instant.now().toString())
                .toJobParameters();

        ExecutionContext stepContext = new ExecutionContext();
        stepContext.put("lastTimeSeconds", Instant.EPOCH);

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(params, stepContext);

        // When
        List<SettlementDetailFindTargetDto> list = new ArrayList<>();
        StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            @SuppressWarnings("unchecked")
            MyBatisPagingItemReader<SettlementDetailFindTargetDto> reader =
                    (MyBatisPagingItemReader<SettlementDetailFindTargetDto>)
                            context.getBean("settlementDetailReader");

            reader.open(stepExecution.getExecutionContext());
            SettlementDetailFindTargetDto item;
            while ((item = reader.read()) != null) {
                list.add(item);
            }
            reader.close();

            return null;
        });

        // Then
        assertThat(list).hasSize(4);
    }

}
