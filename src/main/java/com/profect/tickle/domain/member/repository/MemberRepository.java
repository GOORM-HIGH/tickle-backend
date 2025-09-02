package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("""
        update Member m
           set m.pointBalance = m.pointBalance - :amount
         where m.id = :memberId
           and m.pointBalance >= :amount
    """)
    int tryDeductPoint(@Param("memberId") Long memberId,
                       @Param("amount") int amount);
}
