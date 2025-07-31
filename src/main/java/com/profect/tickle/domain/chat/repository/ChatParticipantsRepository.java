package com.profect.tickle.domain.chat.repository;

import com.profect.tickle.domain.chat.entity.ChatParticipants;
import com.profect.tickle.domain.chat.entity.ChatRoom;
import com.profect.tickle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantsRepository extends JpaRepository<ChatParticipants, Long> {

    // ✅ 기존 메서드들 (모두 올바름)
    Optional<ChatParticipants> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    List<ChatParticipants> findByChatRoomAndStatusTrue(ChatRoom chatRoom);
    List<ChatParticipants> findByMemberAndStatusTrue(Member member);
    int countByChatRoomAndStatusTrue(ChatRoom chatRoom);
    boolean existsByChatRoomAndMemberAndStatusTrue(ChatRoom chatRoom, Member member);

    // ✅ 기존 쿼리 메서드 (올바름)
    @Query("SELECT cp FROM ChatParticipants cp WHERE cp.member.id = :memberId AND cp.status = true")
    List<ChatParticipants> findActiveParticipationsByMemberId(@Param("memberId") Long memberId);

    // ✅ 추가 필요한 메서드들 (ChatParticipantsService에서 사용될 수 있음)

    /**
     * 채팅방 ID로 활성 참여자 목록 조회 (Repository 방식으로도 사용 가능하도록)
     */
    @Query("SELECT cp FROM ChatParticipants cp WHERE cp.chatRoom.id = :chatRoomId AND cp.status = true ORDER BY cp.joinedAt ASC")
    List<ChatParticipants> findActiveByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    /**
     * 회원 ID로 참여 중인 채팅방 목록 조회 (최신 순)
     */
    @Query("SELECT cp FROM ChatParticipants cp JOIN FETCH cp.chatRoom cr WHERE cp.member.id = :memberId AND cp.status = true AND cr.status = true ORDER BY cr.updatedAt DESC")
    List<ChatParticipants> findActiveByMemberIdWithRoom(@Param("memberId") Long memberId);

    // 가장 유용할 것 같은 메서드만 추가
    /**
     * 특정 회원의 특정 채팅방 참여 정보 조회 (활성 상태만)
     */
    @Query("SELECT cp FROM ChatParticipants cp WHERE cp.chatRoom.id = :chatRoomId AND cp.member.id = :memberId AND cp.status = true")
    Optional<ChatParticipants> findActiveByRoomIdAndMemberId(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);

    // 특정 시간 이후에 참여한 회원들 조회
    @Query("SELECT cp FROM ChatParticipants cp WHERE cp.chatRoom.id = :chatRoomId AND cp.joinedAt > :since AND cp.status = true")
    List<ChatParticipants> findRecentParticipants(@Param("chatRoomId") Long chatRoomId, @Param("since") Instant since);
}
