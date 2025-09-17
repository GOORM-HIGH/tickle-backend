package com.profect.tickle.batch.listener;

import java.time.Duration;
import java.time.Instant;

import com.profect.tickle.batch.domain.settlement.dto.SettlementDetailFindTargetDto;
import com.profect.tickle.batch.metadata.BatchMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@StepScope
public class ChunkTimingListener
        implements
        ChunkListener,
        JobExecutionListener,
        StepExecutionListener,
        ItemReadListener<Object>,
        ItemProcessListener<Object, Object>,
        ItemWriteListener<Object>{

    private final BatchMetadataMapper batchMetadataMapper;

    // 정산 Job Parameters
    @Value("#{jobParameters['settlementBatchStartedAt']}")
    private String settlementJobParam;

    // 청크 시작 시 reset
    private Instant chunkStart;
    private long   readNanos;
    private long   processNanos;
    private long   writeNanos;

    // (읽기/처리/쓰기를 측정하기 위한 스택 변수들)
    private ThreadLocal<Instant> readStart    = new ThreadLocal<>();
    private ThreadLocal<Instant> processStart = new ThreadLocal<>();
    private ThreadLocal<Instant> writeStart   = new ThreadLocal<>();

    // * 누적 통계 변수
    private long totalAllNanos;
    private long totalReadNanos;
    private long totalProcessNanos;
    private long totalWriteNanos;
    private int  chunkCount;

    // JobLauncher 실행 후, 호출
    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getJobParameters();
    }

    // StepExecutionListener : 스텝 시작 전에 한번만 호출 -> 누적변수 리셋
    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Step 시작 전에 Job name으로 메타 테이블에서 마지막 배치 시점, 아이디 호출 -> 없을 경우 EPOCH, 0
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        Instant lastTimeSeconds = batchMetadataMapper.findLastProcessedAt(jobName)
                .orElse(Instant.EPOCH);
        Long stepLastId = batchMetadataMapper.findLastProcessedId(jobName)
                .orElse(0L);
        System.out.println("beforeStep에서의 이전 배치의 마지막 아이디 ::::::::: " + stepLastId);

        stepExecution.getExecutionContext().put("lastTimeSeconds", lastTimeSeconds);
        stepExecution.getExecutionContext().put("lastProcessedId", stepLastId);

        // 튜닝 전 테스트용
//        String jobNameTest = stepExecution.getJobExecution().getJobInstance().getJobName();
//        Instant lastTimeSecondsTest = batchMetadataMapper.findLastProcessedAtTest(jobNameTest)
//                .orElse(Instant.EPOCH);
//        stepExecution.getExecutionContext().put("lastTimeSeconds", lastTimeSecondsTest);

        this.chunkCount = 0;
        this.totalAllNanos = 0L;
        this.totalReadNanos = 0L;
        this.totalProcessNanos = 0L;
        this.totalWriteNanos = 0L;
    }

    // 1) 청크가 시작될 때
    @Override
    public void beforeChunk(ChunkContext context) {
        chunkStart   = Instant.now();
        readNanos    = 0L;
        processNanos = 0L;
        writeNanos   = 0L;
    }

    @Override
    public void afterChunk(ChunkContext context) {
        Instant chunkEnd   = Instant.now();
        long totalNanos    = Duration.between(chunkStart, chunkEnd).toNanos();

        // 1) 이번 Chunk 누적
        chunkCount++;
        totalAllNanos     += totalNanos;
        totalReadNanos    += readNanos;
        totalProcessNanos += processNanos;
        totalWriteNanos   += writeNanos;

        // 2) 로그 (기존 그대로 두되, 필요하면 누적 추가)
        System.out.println(String.format(
                "Chunk[%d] ▶ total=%.3f s, read=%.3f s, proc=%.3f s, write=%.3f s",
                context.getStepContext().getStepExecution().getReadCount() /
                        context.getStepContext().getStepExecution().getCommitCount(),
                totalNanos   / 1_000_000_000.0,
                readNanos    / 1_000_000_000.0,
                processNanos / 1_000_000_000.0,
                writeNanos   / 1_000_000_000.0
        ));
    }

    @Override public void afterChunkError(ChunkContext context) { /* 필요시 */ }

    // 2) ItemReader 호출 전/후
    @Override
    public void beforeRead() {
        readStart.set(Instant.now());
    }
    @Override
    public void afterRead(Object item) {
        readNanos += Duration.between(readStart.get(), Instant.now()).toNanos();
    }
    @Override
    public void onReadError(Exception ex) { /* 필요 시 */ }

    // 3) ItemProcessor 호출 전/후
    @Override
    public void beforeProcess(Object item) {
        processStart.set(Instant.now());
    }
    @Override
    public void afterProcess(
            Object item, Object result) {
        processNanos += Duration.between(processStart.get(), Instant.now()).toNanos();
    }
    @Override
    public void onProcessError(
            Object item, Exception ex) { /* 필요 시 */ }

    // 4) ItemWriter 호출 전/후
    @Override
    public void beforeWrite(Chunk<? extends Object> chunk) {
        writeStart.set(Instant.now());
    }
    @Override
    public void afterWrite(Chunk<? extends Object> chunk) {
        writeNanos += Duration.between(writeStart.get(), Instant.now()).toNanos();
    }
    @Override
    public void onWriteError(
            Exception exception, Chunk<? extends Object> chunk) { /* 필요 시 */ }

    // 5) 스텝 끝날 때 최종 누적 통계
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if(stepExecution.getReadCount() == 0) {
            return ExitStatus.COMPLETED;
        }

        // Job Name, Job Params 호출해서 메테 테이블에 마지막 배치 시점 저장
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        Long lastId = stepExecution.getExecutionContext().getLong("lastProcessedId");
        if(jobName.startsWith("settlement")){
            Instant createdAt = Instant.parse(settlementJobParam);
            batchMetadataMapper.upsertLastProcessedAt(jobName, createdAt, lastId);
        }

        // 튜닝 전 테스트용
//        String jobNameTest = stepExecution.getJobExecution().getJobInstance().getJobName();
//        if(jobNameTest.startsWith("settlement")){
//            Instant createdAt = Instant.parse(settlementJobParam);
//            batchMetadataMapper.upsertLastProcessedAtTest(jobNameTest, createdAt);
//        }

        System.out.println("============== STEP 최종 누적 통계 ==============");
        System.out.println(String.format(
                "총 %d개의 Chunk ▶ total=%.3f s, read=%.3f s, proc=%.3f s, write=%.3f s",
                chunkCount,
                totalAllNanos    / 1_000_000_000.0,
                totalReadNanos   / 1_000_000_000.0,
                totalProcessNanos/ 1_000_000_000.0,
                totalWriteNanos  / 1_000_000_000.0
        ));
        return stepExecution.getExitStatus();
    }
}