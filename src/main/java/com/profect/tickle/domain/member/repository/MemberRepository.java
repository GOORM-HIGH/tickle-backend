package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
