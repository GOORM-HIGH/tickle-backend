package com.profect.tickle.domain.chat.repository;

import com.profect.tickle.domain.chat.entity.ChatParticipants;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.member.entity.Member;  // ✅ User → Member로 import 변경
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantsRepository extends JpaRepository<ChatParticipants, Long> {

    // ✅ 수정: User → Member로 변경
    Optional<ChatParticipants> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    // ✅ 수정: User → Member로 변경
    List<ChatParticipants> findByChatRoomAndStatusTrue(ChatRoom chatRoom);

    // ✅ 수정: User → Member로 변경
    List<ChatParticipants> findByMemberAndStatusTrue(Member member);

    // ✅ 수정: User → Member로 변경
    int countByChatRoomAndStatusTrue(ChatRoom chatRoom);

    // ✅ 수정: UserAndStatusTrue → MemberAndStatusTrue
    boolean existsByChatRoomAndMemberAndStatusTrue(ChatRoom chatRoom, Member member);

    // ✅ 수정: UserId → MemberId로 변경
    @Query("SELECT cp FROM ChatParticipants cp WHERE cp.member.id = :memberId AND cp.status = true")
    List<ChatParticipants> findActiveParticipationsByMemberId(@Param("memberId") Long memberId);
}
