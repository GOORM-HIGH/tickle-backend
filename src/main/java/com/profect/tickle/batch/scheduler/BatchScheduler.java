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
//    private final Job settlementWeeklyMonthlyJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("settlementDetailJob") Job settlementDetailJob)// Tasklet
//            @Qualifier("settlementWeeklyMonthlyJob") Job settlementWeeklyMonthlyJob)
    {
        this.jobLauncher = jobLauncher;
        this.settlementDetailJob = settlementDetailJob;
//        this.settlementWeeklyMonthlyJob = settlementWeeklyMonthlyJob;`
    }

    // 매분마다 건별 정산 job 호출
    @Scheduled(cron = "0 * * * * *")
    public void runSettlementDetailDailyJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementDetailCreatedAt", Instant.now().toString(), true)
                .toJobParameters();
        jobLauncher.run(settlementDetailJob, jobParameters);
    }

    // 10분마다 job 호출(주간, 월간)
//    @Scheduled(cron ="59 9,19,29,39,49,59 * * * *")
//    public void runSettlementWeeklyMonthlyJob() throws Exception{
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addDate("runDate", new Date())
//                .toJobParameters();
//        jobLauncher.run(settlementWeeklyMonthlyJob, jobParameters);
//    }
}