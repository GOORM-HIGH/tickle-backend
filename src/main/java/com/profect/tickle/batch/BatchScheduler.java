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
    private final Job settlementJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("settlementJob") Job settlementJob
    ) {
        this.jobLauncher = jobLauncher;
        this.settlementJob = settlementJob;
    }

    // 매 분 0초에 정산 job 호출
    @Scheduled(cron = "0 * * * * *")
    public void runSettlementJob() throws Exception{
        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("runDate", new Date())
                .toJobParameters();
        jobLauncher.run(settlementJob, jobParameters);
    }
}
