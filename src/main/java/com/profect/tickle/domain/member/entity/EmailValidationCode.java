package com.profect.tickle.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailValidationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_validation_id")
    private Long id;

    @Column(name = "email", unique = true, length = 30, nullable = false)
    private String email;

    @Column(name = "email_validation_code", length = 50, nullable = false)
    private String validationCode;

    @Column(name = "email_validation_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "email_expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plus(3, ChronoUnit.MINUTES); // 3분 뒤
    }

    public void regenerateCode(String newValidationCode) {
        this.validationCode = newValidationCode;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plus(3, ChronoUnit.MINUTES);
    }

}
