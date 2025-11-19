package com.profect.tickle.domain.member.repository;

import com.profect.tickle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    interface DeductRow { Integer getPointBalance(); }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
      UPDATE member
         SET member_point_balance = member_point_balance - :amount
       WHERE member_id = :memberId
         AND member_point_balance >= :amount
      RETURNING member_point_balance AS pointBalance
      """, nativeQuery = true)
    List<DeductRow> tryDeductPointReturning(@Param("memberId") Long memberId,
                                            @Param("amount") int amount);

    @Modifying
    @Query("""
        update Member m
           set m.pointBalance = m.pointBalance - :amount
         where m.id = :memberId
           and m.pointBalance >= :amount
    """)
    int tryDeductPoint(@Param("memberId") Long memberId,
                       @Param("amount") int amount);

    @Query("SELECT m.pointBalance FROM Member m WHERE m.id = :memberId")
    Optional<Integer> findPointById(@Param("memberId") Long memberId);
}
