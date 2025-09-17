package com.profect.tickle.batch.domain.settlement.settlementDaily;

import com.profect.tickle.batch.domain.settlement.csvSerializer.SettlementCsvSerializer;
import com.profect.tickle.batch.domain.settlement.custom.KeysetPagingItemReader;
import com.profect.tickle.batch.domain.settlement.dto.SettlementDetailFindTargetDto;
import com.profect.tickle.batch.listener.ChunkTimingListener;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.batch.domain.settlement.dto.SettlementDailyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDaily;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class DailyBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final ChunkTimingListener chunkTimingListener;
    private final SettlementCsvSerializer settlementCsvSerializer;

    /**
     * 일별 정산 Job
     * Job Name: settlementDailyJob
     */
    @Bean
    public Job settlementDailyJob() {
        return new JobBuilder("settlementDailyJob", jobRepository)
                .start(settlementDailyStep())
                .next(settlementDailyUpsertStep())
                .build();
    }

    /**
     * 일별 정산 Step
     * Step Name: settlementDailyStep
     * Chunk Size: 10_000
     */
    @Bean
    public Step settlementDailyStep() {
        return new StepBuilder("settlementDailyStep", jobRepository)
                .<SettlementDailyFindTargetDto, SettlementDaily>chunk(5_000, txManager)
//                .reader(settlementDailyReader(null, null))
                .reader(settlementDailyReader(null, null, null))
                .processor(settlementDailyProcessor())
                .writer(settlementDailyCopyWriter())
//                .writer(settlementDailyCompositeItemWriter())
                .listener((ChunkListener) chunkTimingListener)
                .listener((StepExecutionListener) chunkTimingListener)
                .build();
    }

//    @Bean
//    @StepScope
//    public ItemStreamReader<SettlementDailyFindTargetDto> settlementDailyReader(
//            @Value("#{jobParameters['settlementBatchStartedAt']}") String settlementBatchStartedAt,
//            @Value("#{stepExecutionContext['lastTimeSeconds']}") Instant lastTimeSeconds,
//            @Value("#{stepExecutionContext['lastProcessedId'] ?: 0L}") Long lastProcessedId
//    ) {
//        KeysetPagingItemReader<SettlementDailyFindTargetDto> reader =
//                new KeysetPagingItemReader<>(
//                        sqlSessionTemplate,
//                        "com.profect.tickle.batch.domain.settlement.mapper.SettlementDailyMapper.aggregateFromDetailToDaily",
//                        Instant.parse(settlementBatchStartedAt),
//                        lastTimeSeconds,
//                        lastProcessedId,
//                        5_000,
//                        dto -> dto.getPageMaxId()// 청크 사이즈
//                );
//        return reader;
//    }

    /**
     * 일별 정산 MyBatisPagingItemReader
     * Paging Size: 5_000
     * @param settlementBatchStartedAt: 일별 정산, 배치 메타테이블에 insert, update할 배치 시간(from. JobLauncher)
     * @param lastTimeSeconds: beforStep 단계에서 배치 메타테이블로부터 가져온 마지막 배치 시간(where절 비교용)
     * @return SettlementDailyFindTargetDto
     */
    @Bean
    @StepScope
    public MyBatisPagingItemReader<SettlementDailyFindTargetDto> settlementDailyReader(
            @Value("#{jobParameters['settlementBatchStartedAt']}") String settlementBatchStartedAt,
            @Value("#{stepExecutionContext['lastTimeSeconds']}") Instant lastTimeSeconds,
            @Value("#{stepExecutionContext['lastProcessedId'] ?: 0L}") Long lastProcessedId
    ) {
        Map<String, Object> params = Map.of(
                "now", Instant.parse(settlementBatchStartedAt),
                "lastTimeSeconds", lastTimeSeconds,
                "lastProcessedId", lastProcessedId
        );

        return new MyBatisPagingItemReaderBuilder<SettlementDailyFindTargetDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.profect.tickle.batch.domain.settlement.mapper.SettlementDailyMapper.aggregateFromDetailToDaily")
                .parameterValues(params)
                .pageSize(5_000)
                .maxItemCount(Integer.MAX_VALUE)
                .build();
    }

    /**
     * 일별 정산 ItemProcessor
     * 판매금액, 환불금액, 정산대상금액, 수수료, 대납금액, 환불상태
     * @return SettlementDaily
     */
    @Bean
    public ItemProcessor<SettlementDailyFindTargetDto, SettlementDaily> settlementDailyProcessor() {
        return targetDto -> {
            Member member = memberRepository.findById(targetDto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            Status settlementStatus = null;
            if(targetDto.getSettlementDailyCreatedAt().isBefore(targetDto.getPerformanceEndDate())) {
                settlementStatus = statusProvider.provide(StatusIds.Settlement.SCHEDULED);
            } else if(targetDto.getSettlementDailyCreatedAt().isAfter(targetDto.getPerformanceEndDate())) {
                settlementStatus = statusProvider.provide(StatusIds.Settlement.COMPLETED);
            }

            return SettlementDaily.create(
                    targetDto,
                    member,
                    settlementStatus,
                    targetDto.getSettlementDailyCreatedAt()
            );
        };
    }

    @Bean
    public CompositeItemWriter<SettlementDaily> settlementDailyCompositeItemWriter() {
        CompositeItemWriter<SettlementDaily> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(
                settlementDailyCopyWriter(),
                settlementDailyUpsertAndClear()
        ));
        return writer;
    }

    /**
     * 일별 정산 ItemWriter: COPY(Postgresql COPY ... FROM STDIN 프로토콜)
     * COPY는 upsert가 안되므로 staging용 테이블에 먼저 COPY
     * @return SettlementDaily
     */
    @Bean
    public ItemWriter<SettlementDaily> settlementDailyCopyWriter() {
        return items -> {
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                CopyManager copyManager = new CopyManager((BaseConnection) pgConn);

                String sb = settlementCsvSerializer.dailyCsvSerializer(items);
                String copySql = ""
                        + "COPY settlement_daily_stage("
                        +   "member_id, status_id, performance_title, performance_end_date,"
                        +   "settlement_year, settlement_month, settlement_day,"
                        +   "settlement_daily_sales_amount, settlement_daily_refund_amount,"
                        +   "settlement_daily_gross_amount, contract_charge,"
                        +   "settlement_daily_commission, settlement_daily_net_amount,"
                        +   "settlement_daily_created_at"
                        + ") FROM STDIN WITH (FORMAT csv)";

                try (Reader reader = new StringReader(sb)) {
                    copyManager.copyIn(copySql, reader);
                }
            }
        };
    }

    @Bean
    public ItemWriter<SettlementDaily> settlementDailyUpsertAndClear() {
        return items -> {
            jdbcTemplate.batchUpdate(
                        """
                        INSERT INTO settlement_daily (
                            member_id, status_id, performance_title, performance_end_date,
                            settlement_year, settlement_month, settlement_day,
                            settlement_daily_sales_amount, settlement_daily_refund_amount,
                            settlement_daily_gross_amount, contract_charge, settlement_daily_commission,
                            settlement_daily_net_amount, settlement_daily_created_at
                        )
                        SELECT
                            s.member_id, s.status_id, s.performance_title, s.performance_end_date,
                            s.settlement_year, s.settlement_month, s.settlement_day,
                            s.settlement_daily_sales_amount, s.settlement_daily_refund_amount,
                            s.settlement_daily_gross_amount, s.contract_charge, s.settlement_daily_commission,
                            s.settlement_daily_net_amount, s.settlement_daily_created_at
                        FROM settlement_daily_stage s
                        ON CONFLICT (member_id, performance_title, settlement_year, settlement_month, settlement_day)
                        DO UPDATE SET
                            settlement_daily_sales_amount = settlement_daily.settlement_daily_sales_amount + EXCLUDED.settlement_daily_sales_amount,
                            settlement_daily_refund_amount = settlement_daily.settlement_daily_refund_amount + EXCLUDED.settlement_daily_refund_amount,
                            -- 기존 정산대상금액 + (새로운 판매금액 - 새로운 환불금액)
                            settlement_daily_gross_amount =
                            settlement_daily.settlement_daily_gross_amount + (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount),
                            -- 기존 수수료 + (새로운 판매금액 - 새로운 환불금액) * 적용수수료
                            settlement_daily_commission =
                            settlement_daily.settlement_daily_commission +
                            (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount) * EXCLUDED.contract_charge,
                            -- ((기존 정산대상금액 + (새로운 판매금액 - 새로운 환불금액)) - (기존 수수료 + (새로운 판매금액 - 새로운 환불금액) * 적용수수료)
                            settlement_daily_net_amount =
                            (settlement_daily.settlement_daily_gross_amount + (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount))
                            -
                            (settlement_daily.settlement_daily_commission +
                            (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount) * EXCLUDED.contract_charge),
                            -- 수정 날짜 = 받은 날짜
                            settlement_daily_updated_at = EXCLUDED.settlement_daily_created_at
                        """
            );
            jdbcTemplate.execute("TRUNCATE settlement_daily_stage");
        };
    }

    /**
     * 일별 정산 Upsert
     * Staging 용 테이블로부터 실제 일별 정산 테이블에 upsert
     */
    @Bean
    public Step settlementDailyUpsertStep() {
        return new StepBuilder("upsertFromStaging", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(
                            """
                                INSERT INTO settlement_daily (
                                    member_id, status_id, performance_title, performance_end_date,
                                    settlement_year, settlement_month, settlement_day,
                                    settlement_daily_sales_amount, settlement_daily_refund_amount,
                                    settlement_daily_gross_amount, contract_charge, settlement_daily_commission,
                                    settlement_daily_net_amount, settlement_daily_created_at
                                )
                                SELECT
                                    s.member_id, s.status_id, s.performance_title, s.performance_end_date,
                                    s.settlement_year, s.settlement_month, s.settlement_day,
                                    s.settlement_daily_sales_amount, s.settlement_daily_refund_amount,
                                    s.settlement_daily_gross_amount, s.contract_charge, s.settlement_daily_commission,
                                    s.settlement_daily_net_amount, s.settlement_daily_created_at
                                FROM settlement_daily_stage s
                                ON CONFLICT (member_id, performance_title, settlement_year, settlement_month, settlement_day)
                                DO UPDATE SET
                                    settlement_daily_sales_amount = settlement_daily.settlement_daily_sales_amount + EXCLUDED.settlement_daily_sales_amount,
                                    settlement_daily_refund_amount = settlement_daily.settlement_daily_refund_amount + EXCLUDED.settlement_daily_refund_amount,
                                    -- 기존 정산대상금액 + (새로운 판매금액 - 새로운 환불금액)
                                    settlement_daily_gross_amount =
                                    settlement_daily.settlement_daily_gross_amount + (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount),
                                    -- 기존 수수료 + (새로운 판매금액 - 새로운 환불금액) * 적용수수료
                                    settlement_daily_commission =
                                    settlement_daily.settlement_daily_commission +
                                    (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount) * EXCLUDED.contract_charge,
                                    -- ((기존 정산대상금액 + (새로운 판매금액 - 새로운 환불금액)) - (기존 수수료 + (새로운 판매금액 - 새로운 환불금액) * 적용수수료)
                                    settlement_daily_net_amount =
                                    (settlement_daily.settlement_daily_gross_amount + (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount))
                                    -
                                    (settlement_daily.settlement_daily_commission +
                                    (EXCLUDED.settlement_daily_sales_amount - EXCLUDED.settlement_daily_refund_amount) * EXCLUDED.contract_charge),
                                    -- 수정 날짜 = 받은 날짜
                                    settlement_daily_updated_at = EXCLUDED.settlement_daily_created_at
                                """
                    );
                    jdbcTemplate.execute("TRUNCATE settlement_daily_stage");
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}