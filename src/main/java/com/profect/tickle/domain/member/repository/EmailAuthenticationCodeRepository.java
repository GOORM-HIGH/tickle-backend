package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.EmailAuthenticationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailAuthenticationCodeRepository extends JpaRepository<EmailAuthenticationCode, Long> {
    Optional<EmailAuthenticationCode> findByEmail(String email);
}
