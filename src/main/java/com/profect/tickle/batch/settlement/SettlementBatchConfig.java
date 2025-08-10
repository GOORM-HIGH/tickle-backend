package com.profect.tickle.batch.settlement;

import com.profect.tickle.domain.settlement.service.SettlementDailyService;
import com.profect.tickle.domain.settlement.service.SettlementDetailService;
import com.profect.tickle.domain.settlement.service.SettlementMonthlyService;
import com.profect.tickle.domain.settlement.service.SettlementWeeklyService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
// 스프링배치 작동 시 디폴트로 'transactionManager' 찾아서 주입하려고 함
// 배치 전용으로 만든 txManager 사용하려면 아래처럼 명시해서 사용
@EnableBatchProcessing
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final SettlementDetailService settlementDetailService;
    private final SettlementDailyService settlementDailyService;
    private final SettlementWeeklyService settlementWeeklyService;
    private final SettlementMonthlyService settlementMonthlyService;

    public SettlementBatchConfig(
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager txManager,
            SettlementDetailService settlementDetailService,
            SettlementDailyService settlementDailyService,
            SettlementWeeklyService settlementWeeklyService,
            SettlementMonthlyService settlementMonthlyService
    ) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.settlementDetailService = settlementDetailService;
        this.settlementDailyService = settlementDailyService;
        this.settlementWeeklyService = settlementWeeklyService;
        this.settlementMonthlyService = settlementMonthlyService;
    }

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
        Step dailyStep = new StepBuilder("stepSettlementDaily", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    settlementDailyService.getSettlementDaily();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();

        // 2) JobBuilder로 Job 구성(순차 실행)
        return new JobBuilder("settlementDetailDailyJob", jobRepository)
                .start(detailStep)
                .next(dailyStep)
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
