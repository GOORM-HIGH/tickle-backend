package com.profect.tickle.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementDetailDailyJob;
    private final Job settlementWeeklyMonthlyJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("settlementDetailDailyJob") Job settlementDetailDailyJob,
            @Qualifier("settlementWeeklyMonthlyJob") Job settlementWeeklyMonthlyJob) {
        this.jobLauncher = jobLauncher;
        this.settlementDetailDailyJob = settlementDetailDailyJob;
        this.settlementWeeklyMonthlyJob = settlementWeeklyMonthlyJob;
    }

    // 매분마다 정산 job 호출(건별, 일간)
    @Scheduled(cron = "0 * * * * *")
    public void runSettlementDetailDailyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();
        jobLauncher.run(settlementDetailDailyJob, jobParameters);
    }

    // 10분마다 job 호출(주간, 월간)
    @Scheduled(cron ="59 9,19,29,39,49,59 * * * *")
    // 테스트용 매 분 1초에 호출
//    @Scheduled(cron = "10 * * * * *")
    public void runSettlementWeeklyMonthlyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();
        jobLauncher.run(settlementWeeklyMonthlyJob, jobParameters);
    }
}