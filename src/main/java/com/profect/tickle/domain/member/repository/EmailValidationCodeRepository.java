package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.EmailValidationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailValidationCodeRepository extends JpaRepository<EmailValidationCode, Long> {
    EmailValidationCode findByEmail(String email);
}
