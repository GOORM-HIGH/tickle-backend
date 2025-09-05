package com.profect.tickle.batch.domain.settlement.settlementWeekly;

import com.profect.tickle.batch.domain.settlement.csvSerializer.SettlementCsvSerializer;
import com.profect.tickle.batch.listener.ChunkTimingListener;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.batch.domain.settlement.dto.SettlementWeeklyFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementWeekly;
import com.profect.tickle.batch.domain.settlement.timeUtil.SettlementTimeUtil;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.service.StatusProvider;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
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
import org.springframework.batch.item.ItemWriter;
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
import java.time.LocalDate;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class WeeklyBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final MemberRepository memberRepository;
    private final StatusProvider statusProvider;
    private final ChunkTimingListener chunkTimingListener;
    private final SettlementCsvSerializer settlementCsvSerializer;

    /**
     * 주간 정산 Job
     * Job Name: settlementWeeklyJob
     */
    @Bean
    public Job settlementWeeklyJob() {
        return new JobBuilder("settlementWeeklyJob", jobRepository)
                .start(settlementWeeklyStep())
                .next(settlementWeeklyUpsertStep())
                .build();
    }

    /**
     * 주간 정산 Step
     * Step Name: settlementWeeklyStep
     * Chunk Size: 10_000
     */
    @Bean
    public Step settlementWeeklyStep() {
        return new StepBuilder("settlementWeeklyStep", jobRepository)
                .<SettlementWeeklyFindTargetDto, SettlementWeekly>chunk(10_000, txManager)
                .reader(settlementWeeklyReader(null, null))
                .processor(settlementWeeklyProcessor())
                .writer(settlementWeeklyWriter())
                .listener((ChunkListener) chunkTimingListener)
                .listener((StepExecutionListener) chunkTimingListener)
                .build();
    }

    /**
     * 주간 정산 MyBatisPagingItemReader
     * Paging Size: 50_000
     * @param settlementBatchStartedAt: 건별 정산, 배치 메타테이블에 insert, update할 배치 시간(from. JobLauncher)
     * @param lastTimeSeconds: beforStep 단계에서 배치 메타테이블로부터 가져온 마지막 배치 시간(where절 비교용)
     * @return SettlementWeeklyFindTargetDto
     */
    @Bean
    @StepScope
    public MyBatisPagingItemReader<SettlementWeeklyFindTargetDto> settlementWeeklyReader(
            @Value("#{jobParameters['settlementBatchStartedAt']}") String settlementBatchStartedAt,
            @Value("#{stepExecutionContext['lastTimeSeconds']}")Instant lastTimeSeconds
    ) {
        LocalDate today = SettlementTimeUtil.localDate(Instant.parse(settlementBatchStartedAt));
        SettlementTimeUtil period = SettlementTimeUtil.get(today);
        Map<String, Object> params = Map.of(
                "now", Instant.parse(settlementBatchStartedAt),
                "lastTimeSeconds", lastTimeSeconds,
                "year", period.yearStr(),
                "month", period.monthStr(),
                "day", period.dayOfMonthStr(),
                "week", period.weekOfMonthStr(),
                "from", period.yearStr() + "-" + period.monthStr() + "-" + period.startOfWeek(),
                "to", period.yearStr() + "-" + period.monthStr() + "-" + period.endOfWeek()
        );

        return new MyBatisPagingItemReaderBuilder<SettlementWeeklyFindTargetDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.profect.tickle.batch.domain.settlement.mapper.SettlementWeeklyMapper.aggregateFromDailyToWeekly")
                .parameterValues(params)
                .pageSize(50_000)
                .maxItemCount(Integer.MAX_VALUE)
                .build();
    }

    /**
     * 주간 정산 ItemProcessor
     * 판매금액, 환불금액, 정산대상금액, 수수료, 대납금액, 환불상태
     * @return SettlementWeekly
     */
    @Bean
    public ItemProcessor<SettlementWeeklyFindTargetDto, SettlementWeekly> settlementWeeklyProcessor() {
        return targetDto -> {
            Member member = memberRepository.findById(targetDto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            Status settlementStatus = statusProvider.provide(StatusIds.Settlement.SCHEDULED);

            return SettlementWeekly.create(
                    targetDto,
                    member,
                    settlementStatus
            );
        };
    }

    /**
     * 주간 정산 ItemWriter: COPY(Postgresql COPY ... FROM STDIN 프로토콜)
     * COPY는 upsert가 안되므로 staging용 테이블에 먼저 COPY
     * @return SettlementWeekly
     */
    @Bean
    public ItemWriter<SettlementWeekly> settlementWeeklyWriter() {
        return items -> {
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                CopyManager copyManager = new CopyManager((BaseConnection) pgConn);

                String sb = settlementCsvSerializer.weeklyCsvSerializer(items);

                String copySql = ""
                        + "COPY settlement_weekly_stage("
                        +   "member_id, status_id, performance_title,"
                        +   "settlement_year, settlement_month, settlement_week,"
                        +   "settlement_weekly_sales_amount, settlement_weekly_refund_amount,"
                        +   "settlement_weekly_gross_amount, settlement_weekly_commission,"
                        +   "settlement_weekly_net_amount, settlement_weekly_created_at"
                        + ") FROM STDIN WITH (FORMAT csv)";

                try (Reader reader = new StringReader(sb)) {
                    copyManager.copyIn(copySql, reader);
                }
            }
        };
    }

    /**
     * 주간 정산 Upsert
     * Staging 용 테이블로부터 실제 주간 정산 테이블에 upsert
     */
    @Bean
    public Step settlementWeeklyUpsertStep() {
        return new StepBuilder("upsertFromStaging", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(
                            """
                                INSERT INTO settlement_weekly (
                                    member_id, status_id, performance_title,
                                    settlement_year, settlement_month, settlement_week,
                                    settlement_weekly_sales_amount, settlement_weekly_refund_amount,
                                    settlement_weekly_gross_amount, settlement_weekly_commission,
                                    settlement_weekly_net_amount, settlement_weekly_created_at
                                )
                                SELECT
                                    s.member_id, s.status_id, s.performance_title,
                                    s.settlement_year, s.settlement_month, s.settlement_week,
                                    s.settlement_weekly_sales_amount, s.settlement_weekly_refund_amount,
                                    s.settlement_weekly_gross_amount, s.settlement_weekly_commission,
                                    s.settlement_weekly_net_amount, s.settlement_weekly_created_at
                                FROM settlement_weekly_stage s
                                ON CONFLICT (member_id, performance_title, settlement_year, settlement_month, settlement_week)
                                DO UPDATE SET
                                    settlement_weekly_sales_amount = EXCLUDED.settlement_weekly_sales_amount,
                                    settlement_weekly_refund_amount = EXCLUDED.settlement_weekly_refund_amount,
                                    settlement_weekly_gross_amount = EXCLUDED.settlement_weekly_gross_amount,
                                    settlement_weekly_commission = EXCLUDED.settlement_weekly_commission,
                                    settlement_weekly_net_amount = EXCLUDED.settlement_weekly_net_amount,
                                    settlement_weekly_updated_at = EXCLUDED.settlement_weekly_created_at
                                """
                    );
                    jdbcTemplate.execute("TRUNCATE settlement_weekly_stage");
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
