package com.profect.tickle.domain.chat.repository;

import com.profect.tickle.domain.chat.entity.Chat;
import com.profect.tickle.domain.chat.entity.ChatMessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 채팅방별 메시지 조회 (기본 - 복잡한 조회는 MyBatis 사용 예정)
    List<Chat> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    // 특정 메시지 ID 이후의 메시지들 (실시간 업데이트용)
    List<Chat> findByChatRoomIdAndIdGreaterThanOrderByCreatedAtAsc(Long chatRoomId, Long messageId);

    // 채팅방의 마지막 메시지 조회
    Optional<Chat> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    // 메시지 타입별 조회
    List<Chat> findByChatRoomIdAndMessageTypeOrderByCreatedAtDesc(Long chatRoomId, ChatMessageType messageType);

    // 삭제되지 않은 메시지만 조회
    List<Chat> findByChatRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(Long chatRoomId);

    // 특정 시간 이후의 메시지 개수 (읽지않은 메시지 계산용 - MyBatis에서 더 정교하게 처리 예정)
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.chatRoomId = :roomId AND c.id > :lastReadMessageId")
    int countMessagesSinceLastRead(@Param("roomId") Long roomId, @Param("lastReadMessageId") Long lastReadMessageId);
}
