package com.profect.tickle.batch.domain.settlement;

import com.profect.tickle.batch.domain.settlement.csvSerializer.SettlementCsvSerializer;
import com.profect.tickle.batch.listener.ChunkTimingListener;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.global.exception.BusinessException;
import com.profect.tickle.global.exception.ErrorCode;
import com.profect.tickle.global.status.Status;
import com.profect.tickle.global.status.StatusIds;
import com.profect.tickle.global.status.repository.StatusRepository;
import com.profect.tickle.global.status.service.StatusProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Configuration
// 스프링배치 작동 시 디폴트로 'transactionManager' 찾아서 주입하려고 함
// 배치 전용으로 만든 txManager 사용하려면 아래처럼 명시해서 사용
@EnableBatchProcessing
public class DetailBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;
    private final StatusProvider statusProvider;
    private final DataSource dataSource;
    private final ChunkTimingListener chunkTimingListener;
    private final SettlementCsvSerializer settlementCsvSerializer;

    public DetailBatchConfig(
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager txManager,
            SqlSessionFactory sqlSessionFactory,
            MemberRepository memberRepository,
            StatusRepository statusRepository,
            StatusProvider statusProvider,
            DataSource dataSource,
            ChunkTimingListener chunkTimingListener,
            SettlementCsvSerializer settlementCsvSerializer) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.sqlSessionFactory = sqlSessionFactory;
        this.memberRepository = memberRepository;
        this.statusRepository = statusRepository;
        this.statusProvider = statusProvider;
        this.dataSource = dataSource;
        this.chunkTimingListener = chunkTimingListener;
        this.settlementCsvSerializer = settlementCsvSerializer;
    }

    /**
     * 건별 정산 Job
     * Job Name: settlementDetailJob
     */
    @Bean
    public Job settlementDetailJob() {
        return new JobBuilder("settlementDetailJob", jobRepository)
                .start(settlementDetailStep())
                .build();
    }

    /**
     * 건별 정산 Step
     * Step Name: settlementDetailStep
     * Chunk Size: 10_000
     */
    @Bean
    public Step settlementDetailStep() {
        return new StepBuilder("settlementDetailStep", jobRepository)
                .<SettlementDetailFindTargetDto, SettlementDetail>chunk(10_000, txManager)
                .reader(settlementDetailReader(null, null))
                .processor(settlementDetailProcessor())
                .writer(settlementDetailCopyWriter()) // COPY 방식
//                .writer(settlementDetailWriter()) // batchUpdate 방식
                .listener((ChunkListener) chunkTimingListener)
                .listener((StepExecutionListener) chunkTimingListener)
                .build();
    }

    /**
     * 건별 정산 MyBatisPagingItemReader
     * Paging Size: 50_000
     * @param creatredAtString: 건별 정산, 배치 메타테이블에 insert, update할 배치 시간(from. JobLauncher)
     * @param lastTimeSeconds: beforStep 단계에서 배치 메타테이블로부터 가져온 마지막 배치 시간(where절 비교용)
     * @return SettlementDetailFindTargetDto
     */
    @Bean
    @StepScope
    public MyBatisPagingItemReader<SettlementDetailFindTargetDto> settlementDetailReader(
            @Value("#{jobParameters['settlementDetailCreatedAt']}") String creatredAtString,
            @Value("#{stepExecutionContext['lastTimeSeconds']}") Instant lastTimeSeconds
    ) {
        Map<String, Object> params = Map.of(
                "now", Instant.parse(creatredAtString),
                "lastTimeSeconds", lastTimeSeconds
        );

        return new MyBatisPagingItemReaderBuilder<SettlementDetailFindTargetDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.profect.tickle.domain.settlement.mapper.SettlementDetailMapper.findTargetReservations")
                .parameterValues(params)
                .pageSize(50_000)
                .maxItemCount(Integer.MAX_VALUE)
                .build();
    }

    /**
     * 건별 정산 ItemProcessor
     * 판매금액, 환불금액, 정산대상금액, 수수료, 대납금액, 환불상태
     * @return SettlementDetail
     */
    @Bean
    public ItemProcessor<SettlementDetailFindTargetDto, SettlementDetail> settlementDetailProcessor() {
        return targetDto -> {
            Member member = memberRepository.findById(targetDto.getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            Status reservationStatus = statusRepository.findById(targetDto.getReservationStatusId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STATUS_NOT_FOUND));

            Long reservationPrice = targetDto.getReservationPrice(); // 예매금액
            BigDecimal contractCharge = targetDto.getContractCharge(); // 적용 수수료율

            Long salesAmount = 0L; // 판매금액 초기화
            Long refundAmount = 0L; // 환불금액 초기화

            Status settlementStatus = null; // 정산상태 초기화(14=정산예정, 16=환불청구)
            if (reservationStatus.getId() == 9) {
                salesAmount = reservationPrice;
                settlementStatus = statusProvider.provide(StatusIds.Settlement.SCHEDULED);
            } else if (reservationStatus.getId() == 10) {
                refundAmount = reservationPrice;
                settlementStatus = statusProvider.provide(StatusIds.Settlement.REFUND_REQUESTED);
            }

            // 정산대상금액 = 판매금액
            Long grossAmount = salesAmount;
            // 수수료 = 판매금액 * 정산대상금액
            BigDecimal commission = contractCharge.multiply(BigDecimal.valueOf(grossAmount)).setScale(0, RoundingMode.HALF_UP);
            // 대납금액 = 정산대상금액 - 수수료
            BigDecimal netAmount = BigDecimal.valueOf(grossAmount).subtract(commission);

            // dto에서 공연제목, 예매 종료일시, 예매코드, 적용 수수료율 추출
            return SettlementDetail.create(
                    targetDto,
                    member,
                    settlementStatus,
                    salesAmount,
                    refundAmount,
                    grossAmount,
                    commission.longValueExact(),
                    netAmount.longValueExact(),
                    targetDto.getSettlementDetailCreatedAt()
            );
        };
    }

    /**
     * 건별 정산 ItemWriter: COPY(Postgresql COPY ... FROM STDIN 프로토콜)
     * @return SettlementDetail
     */
    @Bean
    public ItemWriter<SettlementDetail> settlementDetailCopyWriter() {
        return items -> {
            // 1) PGConnection 언래핑
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                CopyManager copyManager = new CopyManager((BaseConnection) pgConn);

                String sb = settlementCsvSerializer.detailCsvSerializer(items);

                // 3) COPY ... FROM STDIN
                String copySql = ""
                        + "COPY settlement_detail("
                        +   "member_id, status_id, performance_title, performance_end_date,"
                        +   "reservation_code, settlement_detail_sales_amount,"
                        +   "settlement_detail_refund_amount, settlement_detail_gross_amount,"
                        +   "contract_charge, settlement_detail_commission,"
                        +   "settlement_detail_net_amount, settlement_detail_created_at"
                        + ") FROM STDIN WITH (FORMAT csv)";

                try (Reader reader = new StringReader(sb)) {
                    copyManager.copyIn(copySql, reader);
                }
            }
        };
    }

    /**
     * 건별 정산 ItemWriter: batchUpdate
     * @return SettlementDetail
     */
    @Bean
    public ItemWriter<SettlementDetail> settlementDetailWriter() {
        final String SQL =
                "INSERT INTO settlement_detail (" +
                        "member_id, status_id, performance_title, performance_end_date, " +
                        "reservation_code, settlement_detail_sales_amount, settlement_detail_refund_amount, " +
                        "settlement_detail_gross_amount, contract_charge, settlement_detail_commission, " +
                        "settlement_detail_net_amount, settlement_detail_created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return items -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SQL)) {

                int count = 0;
                for (SettlementDetail item : items) {
                    ps.setLong(1, item.getMember().getId());
                    ps.setLong(2, item.getStatus().getId());
                    ps.setString(3, item.getPerformanceTitle());
                    ps.setTimestamp(4, Timestamp.from(item.getPerformanceEndDate()));
                    ps.setString(5, item.getReservationCode());
                    ps.setLong(6, item.getSalesAmount());
                    ps.setLong(7, item.getRefundAmount());
                    ps.setLong(8, item.getGrossAmount());
                    ps.setBigDecimal(9, item.getContractCharge());
                    ps.setLong(10, item.getCommission());
                    ps.setLong(11, item.getNetAmount());
                    ps.setTimestamp(12, Timestamp.from(item.getCreatedAt()));

                    ps.addBatch();

                    if (++count % 50_000 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }
        };
    }
}