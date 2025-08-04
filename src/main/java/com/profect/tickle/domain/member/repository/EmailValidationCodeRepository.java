package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.EmailValidationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailValidationCodeRepository extends JpaRepository<EmailValidationCode, Long> {
    Optional<EmailValidationCode> findByEmail(String email);
}
