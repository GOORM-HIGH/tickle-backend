package com.profect.tickle.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementDetailJob;
    private final Job settlementDailyJob;
    private final Job settlementWeeklyJob;
    private final Job settlementMonthlyJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("settlementDetailJob") Job settlementDetailJob,
            @Qualifier("settlementDailyJob") Job settlementDailyJob,
            @Qualifier("settlementWeeklyJob") Job settlementWeeklyJob,
            @Qualifier("settlementMonthlyJob") Job settlementMonthlyJob)
    {
        this.jobLauncher = jobLauncher;
        this.settlementDetailJob = settlementDetailJob;
        this.settlementDailyJob = settlementDailyJob;
        this.settlementWeeklyJob = settlementWeeklyJob;
        this.settlementMonthlyJob = settlementMonthlyJob;
    }

    // 매분마다 건별 정산 job 호출
    @Scheduled(cron = "0 * * * * *")
    public void runSettlementDetailJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementBatchStartedAt", Instant.now().toString(), true)
                .toJobParameters();
        jobLauncher.run(settlementDetailJob, jobParameters);
    }

    // 매분 30초마다 일별 정산 job 호출
    @Scheduled(cron = "30 * * * * *")
    public void runSettlementDailyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementBatchStartedAt", Instant.now().toString(), true)
                .toJobParameters();
        jobLauncher.run(settlementDailyJob, jobParameters);
    }

    // 매분 40초마다 주간 정산 job 호출
    @Scheduled(cron = "40 * * * * *")
    public void runSettlementWeeklyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementBatchStartedAt", Instant.now().toString(), true)
                .toJobParameters();
        jobLauncher.run(settlementWeeklyJob, jobParameters);
    }

    // 매분 50초마다 월간 정산 job 호출
    @Scheduled(cron = "50 * * * * *")
    public void runSettlementMonthlyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementBatchStartedAt", Instant.now().toString(), true)
                .toJobParameters();
        jobLauncher.run(settlementMonthlyJob, jobParameters);
    }
}