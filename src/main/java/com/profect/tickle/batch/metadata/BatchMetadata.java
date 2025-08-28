package com.profect.tickle.batch.metadata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Table(name = "batch_metadata")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class BatchMetadata {

    @Id
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "last_processed_at", nullable = false)
    private Instant lastProcessedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
