package com.profect.tickle.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAuthenticationCode {

    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration TTL = Duration.ofMinutes(3);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_authentication_code_id")
    private Long id;

    @Column(name = "email", unique = true, length = 30, nullable = false)
    private String email;

    @Column(name = "email_authentication_code", length = 50, nullable = false)
    private String validationCode;

    @Column(name = "email_authentication_created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "email_authentication_code_expires_at", nullable = false)
    private Instant expiresAt;

    // 엔티티가 스스로 규칙을 수행 (호출부는 내부 필드를 열람하지 않음)
    public boolean isInResendCooldown(Clock clock) {
        Instant now = Instant.now(clock);
        return createdAt.isAfter(now.minus(RESEND_COOLDOWN));
    }

    public boolean isExpired(Clock clock) {
        return Instant.now(clock).isAfter(expiresAt);
    }

    public Instant resendAvailableAt() {
        return createdAt.plus(RESEND_COOLDOWN);
    }

    public void regenerateCode(String newCode, Clock clock) {
        this.validationCode = newCode;
        Instant now = Instant.now(clock);
        this.createdAt = now;
        this.expiresAt = now.plus(TTL);
    }

    public void assertResendAllowed(Clock clock) {
        if (isInResendCooldown(clock)) {
            throw new IllegalStateException("코드 재전송은 " + resendAvailableAt() + " 이후에 가능합니다.");
        }
    }

    // 팩토리: @PrePersist 없이도 테스트 가능한 생성 방식
    public static EmailAuthenticationCode issue(String email, String code, Clock clock) {
        Instant now = Instant.now(clock);
        return EmailAuthenticationCode.builder()
                .email(email)
                .validationCode(code)
                .createdAt(now)
                .expiresAt(now.plus(TTL))
                .build();
    }
}
