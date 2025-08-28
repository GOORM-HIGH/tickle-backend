package com.profect.tickle.batch.domain.settlement;

import com.profect.tickle.batch.listener.ChunkTimingListener;
import com.profect.tickle.domain.member.entity.Member;
import com.profect.tickle.domain.member.repository.MemberRepository;
import com.profect.tickle.domain.settlement.dto.batch.SettlementDetailFindTargetDto;
import com.profect.tickle.domain.settlement.entity.SettlementDetail;
import com.profect.tickle.domain.settlement.service.SettlementDetailService;
import com.profect.tickle.domain.settlement.service.SettlementMonthlyService;
import com.profect.tickle.domain.settlement.service.SettlementWeeklyService;
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
import org.springframework.batch.repeat.RepeatStatus;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Configuration
// 스프링배치 작동 시 디폴트로 'transactionManager' 찾아서 주입하려고 함
// 배치 전용으로 만든 txManager 사용하려면 아래처럼 명시해서 사용
@EnableBatchProcessing
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SettlementDetailService settlementDetailService;
    private final SettlementWeeklyService settlementWeeklyService;
    private final SettlementMonthlyService settlementMonthlyService;
    private final SqlSessionFactory sqlSessionFactory;
    private final MemberRepository memberRepository;
    private final StatusRepository statusRepository;
    private final StatusProvider statusProvider;
    private final DataSource dataSource;
    private final ChunkTimingListener chunkTimingListener;

    public SettlementBatchConfig(
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager txManager,
            SettlementDetailService settlementDetailService,
            SettlementWeeklyService settlementWeeklyService,
            SettlementMonthlyService settlementMonthlyService,
            SqlSessionFactory sqlSessionFactory,
            MemberRepository memberRepository,
            StatusRepository statusRepository,
            StatusProvider statusProvider,
            DataSource dataSource,
            ChunkTimingListener chunkTimingListener) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.settlementDetailService = settlementDetailService;
        this.settlementWeeklyService = settlementWeeklyService;
        this.settlementMonthlyService = settlementMonthlyService;
        this.sqlSessionFactory = sqlSessionFactory;
        this.memberRepository = memberRepository;
        this.statusRepository = statusRepository;
        this.statusProvider = statusProvider;
        this.dataSource = dataSource;
        this.chunkTimingListener = chunkTimingListener;
    }

    @Bean
    public Job settlementDetailJob() {
        return new JobBuilder("settlementDetailJob", jobRepository)
                .start(detailStep())
                .build();
    }

    @Bean
    public Step detailStep() {
        return new StepBuilder("detailStep", jobRepository)
                .<SettlementDetailFindTargetDto, SettlementDetail>chunk(50_000, txManager)
                .reader(settlementDetailReader(null, null))
                .processor(settlementDetailProcessor())
                .writer(settlementDetailCopyWriter()) // COPY 방식
//                .writer(settlementDetailWriter()) // batchUpdate 방식
                .listener((ChunkListener) chunkTimingListener)
                .listener((StepExecutionListener) chunkTimingListener)
                .build();
    }

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
                .pageSize(1_000_000)
                .maxItemCount(Integer.MAX_VALUE)
                .build();
    }

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

            Long grossAmount = salesAmount; // 정산대상금액 = 판매금액
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
                    Instant.parse(targetDto.getSettlementDetailCreatedAt().toString())
            );
        };
    }

    @Bean
    public ItemWriter<SettlementDetail> settlementDetailCopyWriter() {
        return items -> {
            // 1) PGConnection 언래핑
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                CopyManager copyManager = new CopyManager((BaseConnection) pgConn);

                // 2) StringBuilder 에 CSV 포맷으로 직렬화
                StringBuilder sb = new StringBuilder(items.size() * 200);
                DateTimeFormatter fmt = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        .withZone(ZoneOffset.UTC);

                for (SettlementDetail it : items) {
                    // 숫자/문자/타임스탬프를 CSV 규격으로 찍어준다 (쉼표, 개행)
                    sb.append(it.getMember().getId()).append(',')
                        .append(it.getStatus().getId()).append(',');
                    // 3) performanceTitle (CSV quote 처리)
                    appendCsvField(sb, it.getPerformanceTitle());
                    sb.append(fmt.format(it.getPerformanceEndDate())).append(',')
                        .append(it.getReservationCode()).append(',')
                        .append(it.getSalesAmount()).append(',')
                        .append(it.getRefundAmount()).append(',')
                        .append(it.getGrossAmount()).append(',')
                        .append(it.getContractCharge()).append(',')
                        .append(it.getCommission()).append(',')
                        .append(it.getNetAmount()).append(',')
                        .append(fmt.format(it.getCreatedAt()))
                        .append('\n');
                }

                // 3) COPY INTO STDIN
                String copySql = ""
                        + "COPY settlement_detail("
                        +   "member_id, status_id, performance_title, performance_end_date,"
                        +   "reservation_code, settlement_detail_sales_amount,"
                        +   "settlement_detail_refund_amount, settlement_detail_gross_amount,"
                        +   "contract_charge, settlement_detail_commission,"
                        +   "settlement_detail_net_amount, settlement_detail_created_at"
                        + ") FROM STDIN WITH (FORMAT csv)";

                try (Reader reader = new StringReader(sb.toString())) {
                    copyManager.copyIn(copySql, reader);
                }
            }
        };
    }

    /**
     * CSV 필드로 안전하게 변환해서 StringBuilder 에 붙여 준다.
     */
    private void appendCsvField(StringBuilder sb, String field) {
        if (field == null) {
            sb.append("\"\"");    // 빈 값도 "" 로
        } else {
            // 1) 내부 큰따옴표는 "" 로 이스케이프
            String escaped = field.replace("\"", "\"\"");
            // 2) 전체를 "..." 로 감싸서 append
            sb.append('"')
                    .append(escaped)
                    .append('"');
        }
        sb.append(','); // 다음 필드와 구분
    }

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


    // MVP 단계 Tasklet 구조
    /**
     * 건별, 일간 정산 배치
     */
    @Bean
    public Job settlementDetailDailyJob() {
        // 1) 정산 tasklet 구조 step 생성
        // 건별정산, 배치_스텝 테이블에서 식별자로 구분
        Step detailStep = new StepBuilder("stepSettlementDetail", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    settlementDetailService.getSettlementDetail();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
        // 일간정산
//        Step dailyStep = new StepBuilder("stepSettlementDaily", jobRepository)
//                .tasklet((contribution, chunkContext) -> {
//                    settlementDailyService.getSettlementDaily();
//                    return RepeatStatus.FINISHED;
//                }, txManager)
//                .build();

        // 2) JobBuilder로 Job 구성(순차 실행)
        return new JobBuilder("settlementDetailDailyJob", jobRepository)
                .start(detailStep)
//                .next(dailyStep)
                .build();
    }

    /**
     * 주간, 월간 정산 배치
     */
    @Bean
    public Job settlementWeeklyMonthlyJob() {
        // 주간정산
        Step weeklyStep = new StepBuilder("stepSettlementWeekly", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    settlementWeeklyService.getSettlementWeekly();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();

        // 월간정산
        Step monthlyStep = new StepBuilder("stepSettlementMonthly", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    settlementMonthlyService.getSettlementMonthly();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();

        return new JobBuilder("settlementWeeklyMonthlyJob", jobRepository)
                .start(weeklyStep)
                .next(monthlyStep)
                .build();
    }
}