package com.profect.tickle.domain.chat.repository;

import com.profect.tickle.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 공연별 채팅방 조회
    Optional<ChatRoom> findByPerformanceId(Long performanceId);

    // 활성화된 채팅방 목록
    List<ChatRoom> findByStatusTrue();

    // 특정 공연의 채팅방 존재 여부 확인
    boolean existsByPerformanceId(Long performanceId);

    // 채팅방 상태별 조회
    List<ChatRoom> findByStatus(Boolean status);

    // 채팅방명으로 검색 (LIKE 검색)
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.name LIKE %:name% AND cr.status = true")
    List<ChatRoom> findByNameContainingAndStatusTrue(@Param("name") String name);
}
