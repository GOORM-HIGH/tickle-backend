package com.profect.tickle.batch.metadata;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface BatchMetadataMapper {

    /**
     * job name으로 마지막 배치 시점 찾기
     */
    Optional<Instant> findLastProcessedAt(@Param("jobName") String jobName);

    /**
     * job name으로 마지막 배치 아이디 찾기
     */
    Optional<Long> findLastProcessedId(@Param("jobName") String jobName);

    /**
     * step 끝난 이후에 해당 step 배치 시점 job name으로 기록
     */
    void upsertLastProcessedAt(@Param("jobName") String jobName,
                               @Param("lastTimeSeconds") Instant lastTimeSeconds,
                               @Param("lastProcessedId") Long lastProcessedId);

    // ------------------------------- 튜닝 전 테스트용 -------------------------------------
    /**
     * job name으로 마지막 배치 시점 찾기
     */
    Optional<Instant> findLastProcessedAtTest(@Param("jobName") String jobName);

    /**
     * step 끝난 이후에 해당 step 배치 시점 job name으로 기록
     */
    void upsertLastProcessedAtTest(@Param("jobName") String jobName,
                               @Param("lastTimeSeconds") Instant lastTimeSeconds);

}
