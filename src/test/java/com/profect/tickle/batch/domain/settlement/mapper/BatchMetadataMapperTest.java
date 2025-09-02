package com.profect.tickle.batch.domain.settlement.mapper;

import com.profect.tickle.batch.metadata.BatchMetadataMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@ActiveProfiles("mybatis-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM batch_metadata")
public class BatchMetadataMapperTest {

    @Autowired
    private BatchMetadataMapper batchMetadataMapper;

    @Autowired
    private DataSource dataSource;      // MybatisTest 가 자동으로 등록해 줌

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @DisplayName("배치 메타테이블에서 실행되는 Job Name에 해당되는 마지막 배치 시간을 가져온다.")
    @Test
    void findLastProcessedAtIsNotNullTest() {
        // Given
        String jobName = "settlementDetailJob";
        Instant lastProcessedAtForParam = Instant.parse("2025-08-31T00:00:00Z");
        Instant updatedAtForParam = Instant.parse("2025-08-31T01:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO batch_metadata(job_name, last_processed_at, updated_at) VALUES (?,?,?)",
                "settlementDetailJob",
                Timestamp.from(lastProcessedAtForParam),
                Timestamp.from(updatedAtForParam));

        // When
        Instant lastProcessedAt = batchMetadataMapper.findLastProcessedAt(jobName).orElse(Instant.EPOCH);

        // Then
        assertThat(lastProcessedAt).isNotNull();
        assertThat(lastProcessedAt).isEqualTo(lastProcessedAtForParam);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM batch_metadata")
    @DisplayName("배치 메타테이블에서 실행되는 Job Name에 해당되는 마지막 배치 시간을 가져온다. Null이면 EPOCH를 반환한다.")
    @Test
    void findLastProcessedAtInNullTest() {
        // Given
        String jobName = "settlementDetailJob";
        Instant epoch = Instant.parse("1970-01-01T00:00:00Z");

        // When
        Instant lastProcessedAt = batchMetadataMapper.findLastProcessedAt(jobName).orElse(Instant.EPOCH);

        // Then
        assertThat(lastProcessedAt).isEqualTo(epoch);
    }

    @DisplayName("새로운 시간이 배치 메타테이블에 업데이트 된다.")
    @Test
    void upsertLastProcessedAt() {
        // Given
        String jobName = "settlementDetailJob";
        Instant now = Instant.now();

        // When
        batchMetadataMapper.upsertLastProcessedAt(jobName, now);

        Instant lastProcessedAt = batchMetadataMapper.findLastProcessedAt(jobName).orElse(Instant.EPOCH);

        // Then
        assertThat(lastProcessedAt).isEqualTo(now);
    }
}
