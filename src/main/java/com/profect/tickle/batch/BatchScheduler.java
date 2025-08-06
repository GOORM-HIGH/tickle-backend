package com.profect.tickle.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementDetailDailyJob;
    private final Job settlementWeeklyMonthlyJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("settlementDetailDailyJob") Job settlementDetailDailyJob,
            @Qualifier("settlementWeeklyMonthlyJob") Job settlementWeeklyMonthlyJob
    ) {
        this.jobLauncher = jobLauncher;
        this.settlementDetailDailyJob = settlementDetailDailyJob;
        this.settlementWeeklyMonthlyJob = settlementWeeklyMonthlyJob;
    }

    // 매 분 0초에 정산 job 호출(건별, 일간)
    @Scheduled(cron = "0 * * * * *")
    public void runSettlementDetailDailyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();
        jobLauncher.run(settlementDetailDailyJob, jobParameters);
    }

    // 매일 00시 00분 01초에 job 호출(주간, 월간)
//    @Scheduled(cron ="1 0 0 * * *")
    // 테스트용 매 분 1초에 호출
    @Scheduled(cron = "1 * * * * *")
    public void runSettlementWeeklyMonthlyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();
        jobLauncher.run(settlementWeeklyMonthlyJob, jobParameters);
    }
}
